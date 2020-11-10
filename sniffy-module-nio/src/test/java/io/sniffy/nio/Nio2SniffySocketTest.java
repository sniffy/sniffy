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
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

import static io.sniffy.Threads.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Nio2SniffySocketTest extends BaseSocketTest {

    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProvider.install();
        SniffyAsynchronousChannelProvider.install();

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
            SniffyAsynchronousChannelProvider.uninstall();
            SniffySelectorProvider.uninstall();
            SnifferSocketImplFactory.uninstall();
        }

    }

    @Override
    protected void performSocketOperation() {

        try {

            AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
            channel.connect(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort())).get();

            ByteBuffer requestBuffer = ByteBuffer.wrap(BaseSocketTest.REQUEST);
            ByteBuffer responseBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);

            channel.write(requestBuffer).get();
            requestBuffer.clear();
            channel.read(responseBuffer).get();

            channel.close();

            echoServerRule.joinThreads();

            Assert.assertArrayEquals(BaseSocketTest.REQUEST, echoServerRule.pollReceivedData());
            Assert.assertArrayEquals(BaseSocketTest.RESPONSE, responseBuffer.array());
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        } catch (ExecutionException e) {
            fail(e.getMessage());
        }
    }

}