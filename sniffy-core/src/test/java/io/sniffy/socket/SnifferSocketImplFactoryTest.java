package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.util.ExceptionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Issue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.sniffy.Threads.*;
import static org.junit.Assert.*;

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

    @Test
    public void testInstallTwice() throws Exception {

        SnifferSocketImplFactory.install();
        SnifferSocketImplFactory.install();

        assertNull(SnifferSocketImplFactory.previousSocketImplFactory);

        SnifferSocketImplFactory.uninstall();
    }

    @Test
    @Issue("issues/313")
    public void testInstallTwiceDifferentClassLoaders() throws Exception {

        ClassLoader classLoader = new URLClassLoader(new URL[]{
                SnifferSocketImplFactory.class.getProtectionDomain().getCodeSource().getLocation()
        }, ClassLoader.getSystemClassLoader().getParent());

        Class<?> clazz = classLoader.loadClass(SnifferSocketImplFactory.class.getName());
        Method installMethod = clazz.getMethod("install");
        Method uninstallMethod = clazz.getMethod("uninstall");

        try {
            installMethod.invoke(null);

            try {
                SnifferSocketImplFactory.install();

                assertNull(SnifferSocketImplFactory.previousSocketImplFactory);
            } finally {
                SnifferSocketImplFactory.uninstall();
            }
        } finally {
            uninstallMethod.invoke(null);
        }
    }

    @Before
    public void resetSocketFactory() throws Exception {
        SnifferSocketImplFactory.uninstall();
        Field factoryField = Socket.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        factoryField.set(null, null);
    }

    @After
    public void reinitializeSniffy() throws Exception {
        resetSocketFactory();
        Sniffy.initialize();
    }

}