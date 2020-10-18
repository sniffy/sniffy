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
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.Threads.*;
import static org.junit.Assert.*;

public class NioSniffySocketTest extends BaseSocketTest {


    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();

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

            Thread sinkThread  = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sink.write(ByteBuffer.wrap(new byte[]{1,2,3,5,8}));
                    } catch (IOException e) {
                        exceptionHolder.set(e);
                    }
                }
            });

            sourceThread.start();
            sinkThread.start();
            sourceThread.join();
            sinkThread.join();

            assertArrayEquals(new byte[]{1,2,3,5,8}, targetBuffer.array());

        } catch (Exception e) {
            SniffySelectorProvider.uninstall();
        }

    }

}