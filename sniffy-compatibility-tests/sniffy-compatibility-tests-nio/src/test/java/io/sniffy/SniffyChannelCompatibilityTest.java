package io.sniffy;

import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.socket.SocketMetaData;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class SniffyChannelCompatibilityTest {

    protected final static byte[] RESPONSE = new byte[]{9, 8, 7, 6, 5, 4, 3, 2};
    protected final static byte[] REQUEST = new byte[]{1, 2, 3, 4};

    protected static InetAddress localhost;

    @Rule
    public EchoServerRule echoServerRule = new EchoServerRule(RESPONSE);

    @BeforeClass
    public static void resolveLocalhost() throws UnknownHostException {
        localhost = InetAddress.getByName(null);
    }

    @Test
    public void testBlockSocketChannel() throws Exception {

        try {
            SniffyConfiguration.INSTANCE.setMonitorNio(true);
            Sniffy.initialize();
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(localhost.getHostName(), echoServerRule.getBoundPort(), -1);

            SocketChannel client = SocketChannel.open(new InetSocketAddress(localhost, echoServerRule.getBoundPort()));
            client.write(ByteBuffer.wrap(REQUEST));

            fail("Should have been blocked by Sniffy");
        } catch (ConnectException e) {
            assertTrue(e.getMessage().contains("refused by Sniffy"));
        } finally {
            ConnectionsRegistry.INSTANCE.clear();
            SnifferSocketImplFactory.uninstall();
        }

    }

    @Test
    public void testPipe() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        Sniffy.initialize();

        Pipe pipe = Pipe.open();

        final Pipe.SourceChannel source = pipe.source();
        final Pipe.SinkChannel sink = pipe.sink();

        final ByteBuffer targetBuffer = ByteBuffer.allocate(5);
        final AtomicReference<Exception> exceptionHolder = new AtomicReference<Exception>();

        Thread sourceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    source.read(targetBuffer);
                } catch (IOException e) {
                    exceptionHolder.set(e);
                }
            }
        });

        Thread sinkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sink.write(ByteBuffer.wrap(new byte[]{1, 2, 3, 5, 8}));
                } catch (IOException e) {
                    exceptionHolder.set(e);
                }
            }
        });

        sourceThread.start();
        sinkThread.start();
        sourceThread.join();
        sinkThread.join();

        assertArrayEquals(new byte[]{1, 2, 3, 5, 8}, targetBuffer.array());

    }

    private final static byte[] RESPONSE_FULL = new byte[]{9, 8, 7, 6, 5, 4, 3, 2};
    private final static byte[] RESPONSE_START = new byte[]{9, 8, 7, 6};
    private final static byte[] RESPONSE_END = new byte[]{5, 4, 3, 2};

    private final static byte[] REQUEST_FULL = new byte[]{1, 2, 3, 4};
    private final static byte[] REQUEST_START = new byte[]{1, 2};
    private final static byte[] REQUEST_END = new byte[]{3, 4};

    @Test
    public void testCaptureTrafficGatheringScattering() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setSocketCaptureEnabled(true);

        Sniffy.initialize();

        long prevTimeStamp = System.currentTimeMillis();

        Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());

        try {
            SocketChannel client = SocketChannel.open(new InetSocketAddress(localhost, echoServerRule.getBoundPort()));

            ByteBuffer requestBufferStart = ByteBuffer.wrap(REQUEST_START);
            ByteBuffer requestBufferEnd = ByteBuffer.wrap(REQUEST_END);
            ByteBuffer[] requestBuffers = new ByteBuffer[]{
                    ByteBuffer.allocate(0),
                    requestBufferStart,
                    requestBufferEnd,
                    ByteBuffer.allocate(0)
            };

            ByteBuffer responseBufferStart = ByteBuffer.allocate(RESPONSE_START.length);
            ByteBuffer responseBufferEnd = ByteBuffer.allocate(RESPONSE_END.length);
            ByteBuffer[] responseBuffers = new ByteBuffer[]{
                    ByteBuffer.allocate(0),
                    responseBufferStart,
                    responseBufferEnd,
                    ByteBuffer.allocate(0)
            };

            client.write(requestBuffers);
            client.read(responseBuffers);

            client.close();

            echoServerRule.joinThreads();

            Assert.assertArrayEquals(REQUEST_FULL, echoServerRule.pollReceivedData());
            Assert.assertArrayEquals(RESPONSE_START, responseBufferStart.array());
            Assert.assertArrayEquals(RESPONSE_END, responseBufferEnd.array());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic();

        assertEquals(1, networkTraffic.size());

        for (Map.Entry<SocketMetaData, List<NetworkPacket>> entry : networkTraffic.entrySet()) {

            SocketMetaData socketMetaData = entry.getKey();

            Protocol protocol = socketMetaData.getProtocol(); // say TCP
            String hostName = socketMetaData.getAddress().getHostName(); // say "hostname.acme.com"
            int port = socketMetaData.getAddress().getPort(); // say 443

            assertEquals(Protocol.TCP, protocol);
            assertEquals(localhost.getHostName(), hostName);
            assertEquals(echoServerRule.getBoundPort(), port);

            String stackTrace = socketMetaData.getStackTrace();// optional stacktrace for operation as a String
            ThreadMetaData threadMetaData = socketMetaData.getThreadMetaData();// information about thread which performed the operation

            assertNull(stackTrace);
            assertNull(threadMetaData);

            boolean nextPacketMustBeSent = true;

            List<NetworkPacket> networkPackets = entry.getValue();

            assertEquals(2, networkPackets.size());

            for (NetworkPacket networkPacket : networkPackets) {

                long timestamp = networkPacket.getTimestamp(); // timestamp of operation
                byte[] data = networkPacket.getBytes(); // captured traffic

                assertTrue(timestamp >= prevTimeStamp);
                prevTimeStamp = timestamp;

                assertEquals(nextPacketMustBeSent, networkPacket.isSent());

                if (nextPacketMustBeSent) {
                    assertArrayEquals(REQUEST_FULL, data);
                } else {
                    assertArrayEquals(RESPONSE_FULL, data);
                }

                nextPacketMustBeSent = !nextPacketMustBeSent;

            }

        }

    }

}
