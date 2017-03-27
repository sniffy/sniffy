package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.util.ExceptionUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static io.sniffy.Threads.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SnifferSocketImplFactoryTest extends BaseSocketTest {

    private static class TestSocketImplFactory implements SocketImplFactory {

        private AtomicInteger invocationCounter = new AtomicInteger();

        @Override
        public SocketImpl createSocketImpl() {
            try {
                return SnifferSocketImplFactory.defaultSocketImplClassConstructor.newInstance();
            } catch (Exception e) {
                ExceptionUtil.throwException(e);
                return null;
            } finally {

                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

                boolean serverSocket = false;

                if (null != stackTrace) {
                    for (StackTraceElement ste : stackTrace) {
                        if (ste.getClassName().startsWith("java.net.ServerSocket")) {
                            serverSocket = true;
                        }
                    }
                }

                if (!serverSocket) {
                    invocationCounter.incrementAndGet();
                }
            }
        }

    }

    @Test
    public void testExistingFactory() throws IOException {

        TestSocketImplFactory testSocketImplFactory = new TestSocketImplFactory();

        SnifferSocketImplFactory.uninstall();
        Socket.setSocketImplFactory(testSocketImplFactory);

        SnifferSocketImplFactory.install();

        performSocketOperation();

        assertEquals(1, testSocketImplFactory.invocationCounter.intValue());

        SnifferSocketImplFactory.uninstall();

        performSocketOperation();

        assertEquals(2, testSocketImplFactory.invocationCounter.intValue());

        SnifferSocketImplFactory.install();

    }

    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

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

    @Test
    public void testUninstall() throws Exception {

        Sniffy.initialize();

        SnifferSocketImplFactory.uninstall();

        try (Spy<?> s = Sniffy.spy()) {

            performSocketOperation();

            assertTrue(s.getSocketOperations(CURRENT, null, true).isEmpty());

        }

        SnifferSocketImplFactory.install();

    }

}