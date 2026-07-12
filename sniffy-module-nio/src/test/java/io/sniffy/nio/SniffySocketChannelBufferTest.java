package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.socket.SniffySSLNetworkConnection;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static io.sniffy.nio.NioTestSupport.*;

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
    public void closeAccountsForIncompleteInitialPrefixWithoutDuplicateCapture() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        byte[] prefix = "CONNE".getBytes("US-ASCII");
        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             ServerSocketChannel server = provider.openServerSocketChannel()) {
            server.bind(new InetSocketAddress(BaseSocketTest.localhost, 0));
            SocketChannel delegate = provider.openSocketChannel();
            delegate.connect(server.getLocalAddress());
            SocketChannel accepted = server.accept();
            SniffySocketChannel channel = new SniffySocketChannel(provider, delegate);
            channel.processOutboundBytes(prefix, true);
            assertEquals(prefix.length, channel.pendingInitialOutboundByteCount());
            channel.close();
            accepted.close();
            assertEquals(0, channel.pendingInitialOutboundByteCount());
            assertTrue(channel.isFirstPacketSent());
            assertArrayEquals(prefix, sentTraffic(spy));
        }
    }

    @Test
    public void shutdownOutputFinalizesIncompletePrefixWhenCaptureIsDisabled() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(false).build());
             SniffySocketChannel channel = new SniffySocketChannel(provider, provider.openSocketChannel())) {
            channel.processOutboundBytes("CON".getBytes("US-ASCII"), false);
            try {
                channel.shutdownOutput();
            } catch (java.nio.channels.NotYetConnectedException expected) {
                // The delegate behavior is preserved; the post-write prefix is still accounted for.
            }
            assertEquals(0, channel.pendingInitialOutboundByteCount());
            assertTrue(channel.isFirstPacketSent());
            assertTrue(spy.getNetworkTraffic().isEmpty());
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
    public void proxiedRegistryAllowDelayAndDenyStatusesDriveSubsequentChecks() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        Boolean previous = SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled();
        SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(true);
        try {
            int[] statuses = new int[]{0, 1, -1};
            for (int status : statuses) {
                ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 5443, status);
                try (SniffySocketChannel channel = new SniffySocketChannel(provider, provider.openSocketChannel())) {
                    channel.processOutboundBytes(
                            "CONNECT 127.0.0.1:5443 HTTP/1.1\r\n\r\n".getBytes("US-ASCII"), false);
                    assertEquals(Integer.valueOf(status), channel.connectionStatus());
                    try {
                        channel.checkConnectionAllowed(1);
                        assertTrue("deny status must reject the next connection check", status >= 0);
                    } catch (ConnectException denied) {
                        assertEquals(-1, status);
                    }
                }
            }
        } finally {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 5443, 0);
            SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(previous);
        }
    }

    @Test
    public void correlatesTlsBytesFollowingPartialConnectHandshake() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(BaseSocketTest.localhost, 0));
        AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();
        CountDownLatch acceptedConnection = new CountDownLatch(1);
        Thread serverThread = daemonThread("nio-tls-correlation-server", new Runnable() {
            @Override
            public void run() {
                try (SocketChannel accepted = server.accept()) {
                    acceptedConnection.countDown();
                    while (accepted.read(ByteBuffer.allocate(1)) >= 0) {
                        // Wait for the client to close; this server intentionally sends no response.
                    }
                } catch (Throwable e) {
                    serverFailure.set(e);
                }
            }
        });
        serverThread.start();
        SocketChannel delegate = provider.openSocketChannel();
        delegate.connect(server.getLocalAddress());
        assertTrue("server did not accept the client connection",
                acceptedConnection.await(5, TimeUnit.SECONDS));
        byte[] clientHello = new byte[]{22, 3, 3, 0, 4, 1, 2, 3, 4};
        TestSslConnection sslConnection = new TestSslConnection();
        Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(clientHello), sslConnection);
        try (Spy<?> ignored = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             SniffySocketChannel sniffyChannel = new SniffySocketChannel(provider, delegate)) {
            sniffyChannel.processOutboundBytes(
                    "CONNECT tls.example:443 HTTP/1.1\r\nHost: tls.example\r\n\r\n".getBytes("US-ASCII"), true);
            sniffyChannel.processOutboundBytes(clientHello, true);

            assertEquals(new InetSocketAddress("tls.example", 443), sniffyChannel.getProxiedInetSocketAddress());
            assertSame(sniffyChannel, sslConnection.connection);
        } finally {
            Sniffy.CLIENT_HELLO_CACHE.remove(ByteBuffer.wrap(clientHello));
            try {
                joinOrDumpAndFail(serverThread);
            } finally {
                server.close();
                joinOrDumpAndFail(serverThread);
            }
            assertNull(serverFailure.get());
            SniffySelectorProvider.uninstall();
        }
    }

    @Test
    public void socketViewIsStableAndSharesIncrementalProxyAndTrafficState() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        ServerSocketChannel server = provider.openServerSocketChannel();
        server.bind(new InetSocketAddress(BaseSocketTest.localhost, 0));
        final AtomicReference<Throwable> serverFailure = new AtomicReference<Throwable>();
        final AtomicBoolean listenerClosing = new AtomicBoolean();
        final ByteArrayOutputStream physicalBytes = new ByteArrayOutputStream();
        Thread serverThread = daemonThread("nio-shared-socket-state-server", new Runnable() {
            @Override public void run() {
                try (SocketChannel accepted = server.accept()) {
                    ByteBuffer buffer = ByteBuffer.allocate(128);
                    int read;
                    while ((read = accepted.read(buffer)) >= 0) {
                        if (read > 0) {
                            buffer.flip();
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            physicalBytes.write(bytes, 0, bytes.length);
                            buffer.clear();
                        }
                    }
                } catch (Throwable e) {
                    // Closing the listener during test teardown can interrupt an accept that has
                    // not yet returned on some JDKs. The byte-for-byte assertion below still
                    // proves the accepted connection observed the complete request.
                    if (!(listenerClosing.get() && e instanceof AsynchronousCloseException)) {
                        serverFailure.set(e);
                    }
                }
            }
        });
        serverThread.start();
        SocketChannel raw = provider.openSocketChannel();
        raw.connect(server.getLocalAddress());
        byte[] request = "CONNECT 127.0.0.1:5555 HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n"
                .getBytes("US-ASCII");
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 5555, -42);
        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             SniffySocketChannel channel = new SniffySocketChannel(provider, raw)) {
            Socket first = channel.socket();
            assertSame(first, channel.socket());
            assertSame(channel, first.getChannel());
            OutputStream stream = first.getOutputStream();
            stream.write(request[0]);
            channel.write(ByteBuffer.wrap(request, 1, 3));
            for (int i = 4; i < request.length; i++) stream.write(request[i]);

            assertEquals(new InetSocketAddress("127.0.0.1", 5555), channel.getProxiedInetSocketAddress());
            assertEquals(Integer.valueOf(-42), channel.connectionStatus());
            assertTrue(channel.isFirstPacketSent());

            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            for (List<NetworkPacket> packets : spy.getNetworkTraffic().values()) {
                for (NetworkPacket packet : packets) {
                    if (packet.isSent()) captured.write(packet.getBytes());
                }
            }
            assertArrayEquals(request, captured.toByteArray());
            first.close();
            assertFalse(channel.isOpen());
            assertTrue(first.isClosed());
        } finally {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 5555, 0);
            listenerClosing.set(true);
            server.close();
            joinOrDumpAndFail(serverThread);
            assertNull(serverFailure.get());
            assertArrayEquals(request, physicalBytes.toByteArray());
        }
    }

    @Test
    public void channelAndSocketStreamsShareReadWriteAccountingWithoutDuplicateTraffic() throws Exception {
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();
        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             SocketChannel client = SocketChannel.open(
                     new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()))) {
            SniffySocketChannel channel = (SniffySocketChannel) client;
            Socket socket = channel.socket();
            int requestSplit = BaseSocketTest.REQUEST.length / 2;
            while (requestSplit > 0) {
                int written = channel.write(ByteBuffer.wrap(BaseSocketTest.REQUEST,
                        BaseSocketTest.REQUEST.length / 2 - requestSplit, requestSplit));
                requestSplit -= written;
            }
            int streamOffset = BaseSocketTest.REQUEST.length / 2;
            socket.getOutputStream().write(BaseSocketTest.REQUEST, streamOffset,
                    BaseSocketTest.REQUEST.length - streamOffset);

            int responseSplit = BaseSocketTest.RESPONSE.length / 2;
            ByteBuffer channelResponse = ByteBuffer.allocate(responseSplit);
            while (channelResponse.hasRemaining()) {
                assertTrue(channel.read(channelResponse) >= 0);
            }
            byte[] response = new byte[BaseSocketTest.RESPONSE.length];
            channelResponse.flip();
            channelResponse.get(response, 0, responseSplit);
            InputStream stream = socket.getInputStream();
            int responseOffset = responseSplit;
            while (responseOffset < response.length) {
                int read = stream.read(response, responseOffset, response.length - responseOffset);
                assertTrue(read >= 0);
                responseOffset += read;
            }

            assertArrayEquals(BaseSocketTest.RESPONSE, response);
            assertArrayEquals(BaseSocketTest.REQUEST, traffic(spy, true));
            assertArrayEquals(BaseSocketTest.RESPONSE, traffic(spy, false));
        } finally {
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

    private static byte[] sentTraffic(Spy<?> spy) throws Exception {
        return traffic(spy, true);
    }

    private static byte[] traffic(Spy<?> spy, boolean sent) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (List<NetworkPacket> packets : spy.getNetworkTraffic().values()) {
            for (NetworkPacket packet : packets) {
                if (packet.isSent() == sent) bytes.write(packet.getBytes());
            }
        }
        return bytes.toByteArray();
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
