package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.NetworkPacket;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

}
