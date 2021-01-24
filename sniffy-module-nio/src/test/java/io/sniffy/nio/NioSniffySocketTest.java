package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.SnifferSocketImplFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.Threads.*;
import static org.junit.Assert.*;

public class NioSniffySocketTest extends BaseSocketTest {

    @Test
    public void testSelectionKeys() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        //SniffySelectorProvider.uninstall();
        SniffySelectorProviderModule.initialize();
        //SniffySelectorProvider.install();

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

        //SniffySelectorProvider.uninstall();
        //SniffySelectorProvider.install();
        SniffySelectorProviderModule.initialize();


        try {
            try (Spy<?> s = Sniffy.spy()) {

                performSocketOperation();

                Thread thread = new Thread(this::performSocketOperation);
                thread.start();
                thread.join();

                // Current thread socket operations

                assertEquals(1, (long) s.getSocketOperations(CURRENT, null, true).entrySet().size());

                s.getSocketOperations(CURRENT, null, true).values().stream().findAny().ifPresent((socketStats) -> {
                    Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                    Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
                });

                // Other threads socket operations

                assertEquals(1, s.getSocketOperations(OTHERS, null, true).entrySet().stream().count());

                s.getSocketOperations(OTHERS, null, true).values().stream().findAny().ifPresent((socketStats) -> {
                    Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                    Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
                });

                // Any threads socket operations

                assertEquals(2, s.getSocketOperations(ANY, null, true).entrySet().stream().count());

                s.getSocketOperations(OTHERS, null, true).values().stream().forEach((socketStats) -> {
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

            //SniffySelectorProvider.uninstall();
            //SniffySelectorProvider.install();

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