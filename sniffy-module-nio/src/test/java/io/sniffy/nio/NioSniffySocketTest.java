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
import java.nio.channels.SocketChannel;

import static io.sniffy.Threads.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

}