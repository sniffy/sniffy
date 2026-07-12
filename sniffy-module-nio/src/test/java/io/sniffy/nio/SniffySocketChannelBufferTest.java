package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.socket.SniffySSLNetworkConnection;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SniffySocketChannelBufferTest extends BaseSocketTest {

    @BeforeClass
    public static void openNioInternals() {
        SniffySelectorProviderModule.initialize();
    }

    @Test
    public void copiesHeapBufferWithoutChangingApplicationState() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4, 5});
        buffer.position(4);
        buffer.limit(5);

        assertArrayEquals(new byte[]{1, 2, 3}, SniffySocketChannel.copyBytes(buffer, 1, 3));
        assertEquals(4, buffer.position());
        assertEquals(5, buffer.limit());
    }

    @Test
    public void copiesDirectAndReadOnlyBuffersWithoutChangingState() {
        ByteBuffer direct = ByteBuffer.allocateDirect(6);
        direct.put(new byte[]{10, 11, 12, 13, 14, 15});
        direct.position(5);
        direct.limit(6);
        ByteBuffer readOnly = direct.asReadOnlyBuffer();

        assertArrayEquals(new byte[]{11, 12, 13}, SniffySocketChannel.copyBytes(readOnly, 1, 3));
        assertEquals(5, readOnly.position());
        assertEquals(6, readOnly.limit());
    }

    @Test
    public void gatheringCaptureContainsOnlyTransferredBytes() {
        ByteBuffer ignored = ByteBuffer.wrap(new byte[]{99});
        ByteBuffer first = ByteBuffer.wrap(new byte[]{0, 1, 2, 3});
        ByteBuffer second = ByteBuffer.wrap(new byte[]{4, 5, 6, 7});
        ByteBuffer ignoredTail = ByteBuffer.wrap(new byte[]{88});

        first.position(1); // initial position
        second.position(0); // initial position
        int[] initialPositions = new int[]{first.position(), second.position()};

        // Simulate a partial gathering write: all of the first range and one byte of the second.
        first.position(4);
        second.position(1);

        ByteBuffer[] buffers = new ByteBuffer[]{ignored, first, second, ignoredTail};
        assertArrayEquals(new byte[]{1, 2, 3, 4},
                SniffySocketChannel.copyTransferredBytes(buffers, 1, 2, initialPositions, 4));
        assertEquals(4, first.position());
        assertEquals(1, second.position());
        assertEquals(4, first.limit());
        assertEquals(4, second.limit());
    }

    @Test
    public void zeroLengthCaptureDoesNotMoveBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        assertArrayEquals(new byte[0], SniffySocketChannel.copyBytes(buffer, 0, 0));
        assertEquals(0, buffer.position());
        assertEquals(0, buffer.limit());
    }

    @Test
    public void gatheringWriteAndScatteringReadCaptureExactBytes() throws Exception {
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();
        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             SocketChannel client = SocketChannel.open(
                     new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()))) {
            SniffySocketChannel sniffyChannel = (SniffySocketChannel) client;
            assertEquals(0, client.write(ByteBuffer.allocate(0)));
            assertFalse(sniffyChannel.isFirstPacketSent());

            int split = BaseSocketTest.REQUEST.length / 2;
            ByteBuffer first = ByteBuffer.wrap(BaseSocketTest.REQUEST, 0, split).slice().asReadOnlyBuffer();
            ByteBuffer second = ByteBuffer.allocateDirect(BaseSocketTest.REQUEST.length - split);
            second.put(BaseSocketTest.REQUEST, split, BaseSocketTest.REQUEST.length - split).flip();
            ByteBuffer[] sources = new ByteBuffer[]{ByteBuffer.wrap(new byte[]{99}), first, second,
                    ByteBuffer.wrap(new byte[]{88})};

            while (first.hasRemaining() || second.hasRemaining()) {
                client.write(sources, 1, 2);
            }

            int responseSplit = BaseSocketTest.RESPONSE.length / 2;
            ByteBuffer responseFirst = ByteBuffer.allocateDirect(responseSplit);
            ByteBuffer responseSecond = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length - responseSplit);
            ByteBuffer[] destinations = new ByteBuffer[]{ByteBuffer.allocate(1), responseFirst, responseSecond,
                    ByteBuffer.allocate(1)};
            long totalRead = 0;
            while (totalRead < BaseSocketTest.RESPONSE.length) {
                long read = client.read(destinations, 1, 2);
                if (read < 0) break;
                totalRead += read;
            }

            assertEquals(BaseSocketTest.RESPONSE.length, totalRead);
            assertArrayEquals(BaseSocketTest.RESPONSE, join(responseFirst, responseSecond));
            assertEquals(0, sources[0].position());
            assertEquals(0, sources[3].position());
            assertEquals(0, destinations[0].position());
            assertEquals(0, destinations[3].position());

            ByteArrayOutputStream sent = new ByteArrayOutputStream();
            ByteArrayOutputStream received = new ByteArrayOutputStream();
            for (List<NetworkPacket> packets : spy.getNetworkTraffic().values()) {
                for (NetworkPacket packet : packets) {
                    byte[] bytes = packet.getBytes();
                    (packet.isSent() ? sent : received).write(bytes, 0, bytes.length);
                }
            }
            assertArrayEquals(BaseSocketTest.REQUEST, sent.toByteArray());
            assertArrayEquals(BaseSocketTest.RESPONSE, received.toByteArray());
        } finally {
            SniffySelectorProvider.uninstall();
        }
    }

    @Test
    public void detectsHttpConnectAcrossPartialWritesWhenCaptureIsDisabled() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        try (Spy<?> ignored = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(false).build());
             SniffySocketChannel sniffyChannel = new SniffySocketChannel(provider, provider.openSocketChannel())) {

            sniffyChannel.processOutboundBytes("CON".getBytes("US-ASCII"), false);
            assertFalse(sniffyChannel.isFirstPacketSent());
            assertEquals(3, sniffyChannel.pendingInitialOutboundByteCount());
            assertNull(sniffyChannel.getProxiedInetSocketAddress());

            sniffyChannel.processOutboundBytes("NECT example.com:".getBytes("US-ASCII"), false);
            sniffyChannel.processOutboundBytes(
                    "443 HTTP/1.1\r\nHost: example.com\r\n\r\n".getBytes("US-ASCII"), false);

            assertEquals(new InetSocketAddress("example.com", 443), sniffyChannel.getProxiedInetSocketAddress());
            assertTrue(sniffyChannel.isFirstPacketSent());
            assertEquals(0, sniffyChannel.pendingInitialOutboundByteCount());
            assertEquals(0, ignored.getNetworkTraffic().size());
        } finally {
            SniffySelectorProvider.uninstall();
        }
    }

    @Test
    public void proxyCandidateBufferIsBounded() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        try (SniffySocketChannel sniffyChannel = new SniffySocketChannel(provider, provider.openSocketChannel())) {
            byte[] oversizedCandidate = new byte[9000];
            byte[] prefix = "CONNECT ".getBytes("US-ASCII");
            System.arraycopy(prefix, 0, oversizedCandidate, 0, prefix.length);
            for (int i = prefix.length; i < oversizedCandidate.length; i++) {
                oversizedCandidate[i] = 'a';
            }
            sniffyChannel.processOutboundBytes(oversizedCandidate, false);

            assertTrue(sniffyChannel.isFirstPacketSent());
            assertEquals(0, sniffyChannel.pendingInitialOutboundByteCount());
        } finally {
            SniffySelectorProvider.uninstall();
        }
    }

    @Test
    public void directHttpIsRejectedAsProxyCandidateImmediately() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        try (SniffySocketChannel sniffyChannel = new SniffySocketChannel(provider, provider.openSocketChannel())) {
            sniffyChannel.processOutboundBytes("GET / HTTP/1.1\r\n\r\n".getBytes("US-ASCII"), false);
            assertTrue(sniffyChannel.isFirstPacketSent());
            assertNull(sniffyChannel.getProxiedInetSocketAddress());
        } finally {
            SniffySelectorProvider.uninstall();
        }
    }

    @Test
    public void correlatesTlsBytesFollowingPartialConnectHandshake() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(BaseSocketTest.localhost, 0));
        AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (SocketChannel accepted = server.accept()) {
                    while (accepted.read(ByteBuffer.allocate(1)) >= 0) {
                        // Wait for the client to close; this server intentionally sends no response.
                    }
                } catch (Throwable e) {
                    serverFailure.set(e);
                }
            }
        }, "nio-tls-correlation-server");
        serverThread.start();
        SocketChannel delegate = provider.openSocketChannel();
        delegate.connect(server.getLocalAddress());
        byte[] clientHello = new byte[]{22, 3, 3, 0, 4, 1, 2, 3, 4};
        TestSslConnection sslConnection = new TestSslConnection();
        Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(clientHello), sslConnection);
        try (Spy<?> ignored = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             SniffySocketChannel sniffyChannel = new SniffySocketChannel(provider, delegate)) {
            sniffyChannel.processOutboundBytes("CONNECT tls.example:443 HTTP/1.1\r\n".getBytes("US-ASCII"), true);
            sniffyChannel.processOutboundBytes(clientHello, true);

            assertEquals(new InetSocketAddress("tls.example", 443), sniffyChannel.getProxiedInetSocketAddress());
            assertSame(sniffyChannel, sslConnection.connection);
        } finally {
            Sniffy.CLIENT_HELLO_CACHE.remove(ByteBuffer.wrap(clientHello));
            server.close();
            serverThread.join();
            assertNull(serverFailure.get());
            SniffySelectorProvider.uninstall();
        }
    }

    private static byte[] join(ByteBuffer first, ByteBuffer second) {
        ByteBuffer firstCopy = first.duplicate();
        ByteBuffer secondCopy = second.duplicate();
        firstCopy.flip();
        secondCopy.flip();
        byte[] bytes = new byte[firstCopy.remaining() + secondCopy.remaining()];
        firstCopy.get(bytes, 0, firstCopy.remaining());
        secondCopy.get(bytes, first.position(), secondCopy.remaining());
        return bytes;
    }

    private static class TestSslConnection implements SniffySSLNetworkConnection {

        private SniffyNetworkConnection connection;

        @Override
        public SniffyNetworkConnection getSniffyNetworkConnection() {
            return connection;
        }

        @Override
        public void setSniffyNetworkConnection(SniffyNetworkConnection sniffyNetworkConnection) {
            this.connection = sniffyNetworkConnection;
        }
    }

}
