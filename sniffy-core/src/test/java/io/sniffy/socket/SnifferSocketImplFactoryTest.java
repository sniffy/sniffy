package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionUtil;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.Threads.*;
import static org.junit.Assert.*;

public class SnifferSocketImplFactoryTest extends BaseSocketTest {

    private static class TestSocketImplFactory implements SocketImplFactory {

        private AtomicInteger invocationCounter = new AtomicInteger();

        @Override
        public SocketImpl createSocketImpl() {

            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            boolean serverSocket = false;

            if (null != stackTrace) {
                for (StackTraceElement ste : stackTrace) {
                    if (ste.getClassName().startsWith("java.net.ServerSocket")) {
                        serverSocket = true;
                    }
                }
            }

            try {
                if (null != SnifferSocketImplFactory.defaultSocketImplClassConstructor) {
                    return SnifferSocketImplFactory.defaultSocketImplClassConstructor.newInstance();
                }
                if (null != SnifferSocketImplFactory.defaultSocketImplFactoryMethod) {
                    return (SocketImpl) SnifferSocketImplFactory.defaultSocketImplFactoryMethod.invoke(null, serverSocket);
                }
                return null;
            } catch (Exception e) {
                ExceptionUtil.throwException(e);
                return null;
            } finally {
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

            AtomicReference<Throwable> throwableHolder = new AtomicReference<>();

            Thread thread = new Thread(() -> {
                try {
                    performSocketOperation();
                } catch (Throwable e) {
                    throwableHolder.set(e);
                }
            });
            thread.start();
            thread.join();

            assertNull(throwableHolder.get());

            // Current thread socket operations

            assertEquals(1, (long) s.getSocketOperations(CURRENT, true).entrySet().size());

            s.getSocketOperations(CURRENT, true).values().stream().findAny().ifPresent((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

            // Other threads socket operations

            assertEquals(1, s.getSocketOperations(OTHERS, true).entrySet().stream().count());

            s.getSocketOperations(OTHERS, true).values().stream().findAny().ifPresent((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

            // Any threads socket operations

            assertEquals(2, s.getSocketOperations(ANY, true).entrySet().stream().count());

            s.getSocketOperations(OTHERS, true).values().stream().forEach((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

        }

    }

    @Test
    public void testServerSocket() throws Exception {

        SnifferSocketImplFactory.uninstall();

        try {
            SnifferSocketImplFactory.install();

            int boundPort = 10000;

            ServerSocket serverSocket = null;

            for (int i = 0; i < 10; i++, boundPort++) {
                try {
                    serverSocket = new ServerSocket(boundPort, 50, InetAddress.getByName(null));
                    serverSocket.setReuseAddress(true);
                    break;
                } catch (IOException e) {
                    try {
                        if (null != serverSocket) {
                            serverSocket.close();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            AtomicReference<Exception> exceptionHolder = new AtomicReference<>();
            AtomicReference<Socket> socketHolder = new AtomicReference<>();

            final ServerSocket boundServerSocket = serverSocket;

            SocketImpl boundServerSocketImpl = ReflectionUtil.getField(ServerSocket.class, boundServerSocket, "impl");
            assertFalse(boundServerSocketImpl instanceof SnifferSocketImpl);

            Thread serverThread = new Thread(new Runnable() {

                @Override
                public void run() {

                    try {

                        Socket accept = boundServerSocket.accept();
                        socketHolder.set(accept);

                    } catch (Exception e) {
                        exceptionHolder.set(e);
                    }

                }

            });

            serverThread.start();

            Socket clientSocket = new Socket(localhost, boundPort);
            assertTrue(clientSocket.isConnected());

            SocketImpl clientSocketImpl = ReflectionUtil.getField(Socket.class, clientSocket, "impl");
            assertTrue(clientSocketImpl instanceof SnifferSocketImpl);

            assertNull(exceptionHolder.get());

            Socket socket = socketHolder.get();
            SocketImpl socketImpl = ReflectionUtil.getField(Socket.class, socket, "impl");
            assertFalse(socketImpl instanceof SnifferSocketImpl);

            clientSocket.close();

            serverThread.join();

        } finally {
            SnifferSocketImplFactory.uninstall();
        }

    }

    @Test
    public void testUninstall() throws Exception {

        Sniffy.initialize();

        SnifferSocketImplFactory.uninstall();

        try (Spy<?> s = Sniffy.spy()) {

            performSocketOperation();

            assertTrue(s.getSocketOperations(CURRENT, true).isEmpty());

        }

        SnifferSocketImplFactory.install();

    }

}