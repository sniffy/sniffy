package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.ReflectionUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.Threads.*;
import static org.junit.Assert.*;

public class NioSniffySocketTest extends BaseSocketTest {

    @Test
    public void testComplexLogic() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();

        try (Selector selector = Selector.open()) {

            assertTrue(selector instanceof SniffySelector);
            SniffySelector sniffySelector = (SniffySelector) selector;

            SocketChannel socketChannel1 = connectToLocalHost(selector);
            SocketChannel socketChannel2 = connectToLocalHost(selector);

            AbstractSelector delegateSelector = sniffySelector.getDelegate();

            // select should process deregister queue
            assertEquals(0, selector.selectNow());

            assertEquals(0, delegateSelector.keys().size());
            assertEquals(0, delegateSelector.selectedKeys().size());

            assertEquals(0, sniffySelector.keys().size());
            assertEquals(0, sniffySelector.selectedKeys().size());

            long attempts = JVMUtil.invokeGarbageCollector();

            assertNotNull(socketChannel1);
            assertNotNull(socketChannel2);

            SelectionKey[] channel1Keys = ReflectionUtil.getField(AbstractSelectableChannel.class, socketChannel1, "keys");
            for (SelectionKey channel1Key : channel1Keys) {
                assertNull("Failed to clear keys in SniffySocketChannel after " + attempts + " attempts", channel1Key);
            }

            SelectionKey[] channel2Keys = ReflectionUtil.getField(AbstractSelectableChannel.class, socketChannel2, "keys");
            for (SelectionKey channel2Key : channel2Keys) {
                assertNull("Failed to clear keys in SniffySocketChannel after " + attempts + " attempts", channel2Key);
            }

        } finally {
            SnifferSocketImplFactory.uninstall();
            SniffySelectorProvider.uninstall();
        }

    }

    // TODO: test SniffySelector.close()

    @Test
    public void testSelectionKeys() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();

        try {
            Selector selector = Selector.open();

            connectToLocalHost(selector);
        } finally {
            SnifferSocketImplFactory.uninstall();
            SniffySelectorProvider.uninstall();
        }

    }

    private SocketChannel connectToLocalHost(Selector selector) throws IOException {
        ByteBuffer responseBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);

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

        socketChannel.close();

        return socketChannel;
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

                assertEquals(1, s.getSocketOperations(CURRENT, true).entrySet().size());

                s.getSocketOperations(CURRENT, true).values().stream().findAny().ifPresent((socketStats) -> {
                    Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                    Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
                });

                // Other threads socket operations

                assertEquals(1, s.getSocketOperations(OTHERS, true).entrySet().size());

                s.getSocketOperations(OTHERS, true).values().stream().findAny().ifPresent((socketStats) -> {
                    Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                    Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
                });

                // Any threads socket operations

                assertEquals(2, s.getSocketOperations(ANY, true).entrySet().size());

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