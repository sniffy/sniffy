package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.nio.SniffySelectorProvider;
import io.sniffy.util.ExceptionUtil;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

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
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

            // Other threads socket operations

            assertEquals(1, s.getSocketOperations(OTHERS, null, true).entrySet().stream().count());

            s.getSocketOperations(OTHERS, null, true).values().stream().findAny().ifPresent((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

            // Any threads socket operations

            assertEquals(2, s.getSocketOperations(ANY, null, true).entrySet().stream().count());

            s.getSocketOperations(OTHERS, null, true).values().stream().forEach((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

        }

    }

    @Override
    protected void performSocketOperation() {

        try {
            SocketChannel client = SocketChannel.open(new InetSocketAddress(localhost, echoServerRule.getBoundPort()));

            ByteBuffer requestBuffer = ByteBuffer.wrap(REQUEST);
            ByteBuffer responseBuffer = ByteBuffer.allocate(RESPONSE.length);

            client.write(requestBuffer);
            requestBuffer.clear();
            client.read(responseBuffer);

            client.close();

            echoServerRule.joinThreads();

            assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
            assertArrayEquals(RESPONSE, responseBuffer.array());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}