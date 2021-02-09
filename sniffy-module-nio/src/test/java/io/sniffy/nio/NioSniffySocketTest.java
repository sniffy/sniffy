package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.ThreadMetaData;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.*;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Issue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.Threads.*;
import static org.junit.Assert.*;

public class NioSniffySocketTest extends BaseSocketTest {

    @Test
    public void testSelectionKeys() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();

        try {
            ByteBuffer responseBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);

            Selector selector = Selector.open();

            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            socketChannel.connect(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()));

            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            selectorLoop:
            while (true) {
                // Wait for an event one of the registered channels
                selector.select();

                // Iterate over the set of keys for which events are available
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    // Check what event is available and deal with it
                    if (key.isConnectable()) {
                        SocketChannel channel = (SocketChannel) key.channel();

                        // Finish the connection. If the connection operation failed
                        // this will raise an IOException.
                        try {
                            channel.finishConnect();
                        } catch (IOException e) {
                            // Cancel the channel's registration with our selector
                            e.printStackTrace();
                            key.cancel();
                            break selectorLoop;
                        }

                        // Register an interest in writing on this channel
                        //key.interestOps(SelectionKey.OP_WRITE);
                        key.interestOps(0);
                        channel.register(selector, SelectionKey.OP_WRITE);
                    } else if (key.isReadable()) {

                        SocketChannel channel = (SocketChannel) key.channel();

                        // Attempt to read off the channel
                        int numRead;
                        try {
                            numRead = channel.read(responseBuffer);
                        } catch (IOException e) {
                            // The remote forcibly closed the connection, cancel
                            // the selection key and close the channel.
                            key.cancel();
                            channel.close();
                            break selectorLoop;
                        }

                        if (!responseBuffer.hasRemaining()) {
                            // Entire response consumed
                            key.channel().close();
                            key.cancel();
                            break selectorLoop;
                        }

                        if (numRead == -1) {
                            // Remote entity shut the socket down cleanly. Do the
                            // same from our end and cancel the channel.
                            key.channel().close();
                            key.cancel();
                            break selectorLoop;
                        }

                    } else if (key.isWritable()) {
                        SocketChannel channel = (SocketChannel) key.channel();

                        ByteBuffer requestBuffer = ByteBuffer.wrap(BaseSocketTest.REQUEST);

                        while (requestBuffer.remaining() > 0) {
                            channel.write(requestBuffer);
                        }

                        key.interestOps(0);
                        channel.register(selector, SelectionKey.OP_READ);

                    }

                }

            }

            Assert.assertArrayEquals(BaseSocketTest.RESPONSE, responseBuffer.array());
        } finally {
            SnifferSocketImplFactory.uninstall();
            SniffySelectorProvider.uninstall();
        }

    }

    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();

        try {
            try (Spy<?> s = Sniffy.spy()) {

                performSocketOperation();

                Thread thread = new Thread(this::performSocketOperation);
                thread.start();
                thread.join();

                // Current thread socket operations

                assertEquals(1, (long) s.getSocketOperations(CURRENT, true).entrySet().size());

                s.getSocketOperations(CURRENT, true).values().stream().findAny().ifPresent((socketStats) -> {
                    Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                    Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
                });

                // Other threads socket operations

                assertEquals(1, s.getSocketOperations(OTHERS, true).entrySet().stream().count());

                s.getSocketOperations(OTHERS, true).values().stream().findAny().ifPresent((socketStats) -> {
                    Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                    Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
                });

                // Any threads socket operations

                assertEquals(2, s.getSocketOperations(ANY, true).entrySet().stream().count());

                s.getSocketOperations(OTHERS, true).values().stream().forEach((socketStats) -> {
                    Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                    Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
                });

            }
        } finally {
            SnifferSocketImplFactory.uninstall();
            SniffySelectorProvider.uninstall();
        }

    }

    @Test
    @Issue("issues/402")
    public void testCaptureTraffic() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setSocketCaptureEnabled(true);

        Sniffy.initialize();

        long prevTimeStamp = System.currentTimeMillis();

        try {
            try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {

                performSocketOperation();

                Thread thread = new Thread(this::performSocketOperation);
                thread.start();
                thread.join();

                Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic();

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

                    assertEquals(4, networkPackets.size());

                    for (NetworkPacket networkPacket : networkPackets) {

                        long timestamp = networkPacket.getTimestamp(); // timestamp of operation
                        byte[] data = networkPacket.getBytes(); // captured traffic

                        assertTrue(timestamp >= prevTimeStamp);
                        prevTimeStamp = timestamp;

                        assertEquals(nextPacketMustBeSent, networkPacket.isSent());

                        if (nextPacketMustBeSent) {
                            assertArrayEquals(REQUEST, data);
                        } else {
                            assertArrayEquals(RESPONSE, data);
                        }

                        nextPacketMustBeSent = !nextPacketMustBeSent;

                    }

                }

            }
        } finally {
            SnifferSocketImplFactory.uninstall();
            SniffySelectorProvider.uninstall();
        }

    }

    private final static byte[] RESPONSE_FULL = new byte[] {9, 8, 7, 6, 5, 4, 3, 2};
    private final static byte[] RESPONSE_START = new byte[] {9, 8, 7, 6};
    private final static byte[] RESPONSE_END = new byte[] {5, 4, 3, 2};

    private final static byte[] REQUEST_FULL = new byte[] {1, 2, 3, 4};
    private final static byte[] REQUEST_START = new byte[] {1, 2};
    private final static byte[] REQUEST_END = new byte[] {3, 4};

    @Test
    @Issue("issues/402")
    public void testCaptureTrafficGatheringScattering() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setSocketCaptureEnabled(true);

        Sniffy.initialize();

        long prevTimeStamp = System.currentTimeMillis();

        try {
            try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {

                try {
                    SocketChannel client = SocketChannel.open(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()));

                    ByteBuffer requestBufferStart = ByteBuffer.wrap(REQUEST_START);
                    ByteBuffer requestBufferEnd = ByteBuffer.wrap(REQUEST_END);
                    ByteBuffer[] requestBuffers = new ByteBuffer[] {
                            ByteBuffer.allocate(0),
                            requestBufferStart,
                            requestBufferEnd,
                            ByteBuffer.allocate(0)
                    };

                    ByteBuffer responseBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);

                    client.write(requestBuffers);
                    client.read(responseBuffer);

                    client.close();

                    echoServerRule.joinThreads();

                    Assert.assertArrayEquals(BaseSocketTest.REQUEST, echoServerRule.pollReceivedData());
                    Assert.assertArrayEquals(BaseSocketTest.RESPONSE, responseBuffer.array());
                } catch (IOException e) {
                    fail(e.getMessage());
                }

                Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic();

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
                            assertArrayEquals(REQUEST, data);
                        } else {
                            assertArrayEquals(RESPONSE, data);
                        }

                        nextPacketMustBeSent = !nextPacketMustBeSent;

                    }

                }

            }
        } finally {
            SnifferSocketImplFactory.uninstall();
            SniffySelectorProvider.uninstall();
        }

    }

    @Override
    protected void performSocketOperation() {

        try {
            SocketChannel client = SocketChannel.open(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()));

            ByteBuffer requestBuffer = ByteBuffer.wrap(BaseSocketTest.REQUEST);
            ByteBuffer responseBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);

            client.write(requestBuffer);
            requestBuffer.clear();
            client.read(responseBuffer);

            client.close();

            echoServerRule.joinThreads();

            Assert.assertArrayEquals(BaseSocketTest.REQUEST, echoServerRule.pollReceivedData());
            Assert.assertArrayEquals(BaseSocketTest.RESPONSE, responseBuffer.array());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testPipe() {

        try {
            SniffySelectorProviderModule.initialize();
            SniffySelectorProvider.uninstall();
            SniffySelectorProvider.install();

            Pipe pipe = Pipe.open();

            Pipe.SourceChannel source = pipe.source();
            Pipe.SinkChannel sink = pipe.sink();

            final ByteBuffer targetBuffer = ByteBuffer.allocate(5);
            final AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

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

        } catch (Exception e) {
            SniffySelectorProvider.uninstall();
        } finally {
            SniffySelectorProvider.uninstall();
        }

    }

}