package io.sniffy.nio;

import org.junit.Test;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NioTestIsolationTest {

    @Test
    public void functionalForkStartsWithSniffyProviderInstalled() {
        NioFunctionalTestEnvironment.assertGloballyInstalled();
        assertSame(NioFunctionalTestEnvironment.installedProvider(), SelectorProvider.provider());
    }

    @Test
    public void functionalTestsNeverObserveOriginalProvider() throws Exception {
        try (SocketChannel socket = SocketChannel.open();
             ServerSocketChannel server = ServerSocketChannel.open()) {
            assertTrue(socket instanceof SniffySocketChannel);
            assertTrue(server instanceof SniffyServerSocketChannel);
            NioFunctionalTestEnvironment.assertGloballyInstalled();
        }
    }

    @Test
    public void rawDelegateFixturesDoNotMutateGlobalProvider() throws Exception {
        SelectorProvider original = NioFunctionalTestEnvironment.originalProvider();
        try (SocketChannel rawSocket = original.openSocketChannel();
             ServerSocketChannel rawServer = original.openServerSocketChannel();
             AbstractSelector rawSelector = NioFunctionalTestEnvironment.openRawSelector()) {
            assertFalse(rawSocket instanceof SniffySocketChannel);
            assertFalse(rawServer instanceof SniffyServerSocketChannel);
            assertFalse(rawSelector instanceof SniffySelector);
            assertFalse(SniffySelectorProvider.isDelegateSelectorConstruction());
            assertSame(NioFunctionalTestEnvironment.installedProvider(), SelectorProvider.provider());
        }
    }

    @Test
    public void cleanupUtilityRestoresStateAfterInjectedFailure() throws Exception {
        final AtomicInteger state = new AtomicInteger(7);
        NioTestStateScope scope = new NioTestStateScope()
                .restore(new NioTestStateScope.Cleanup() {
                    @Override public void run() { state.set(7); }
                })
                .restore(new NioTestStateScope.Cleanup() {
                    @Override public void run() { throw new IllegalStateException("second cleanup"); }
                })
                .restore(new NioTestStateScope.Cleanup() {
                    @Override public void run() { throw new IllegalArgumentException("first cleanup"); }
                });
        state.set(99);
        try {
            scope.close();
            fail("cleanup failure expected");
        } catch (IllegalArgumentException expected) {
            assertEquals("first cleanup", expected.getMessage());
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("second cleanup", expected.getSuppressed()[0].getMessage());
        }
        assertEquals(7, state.get());
    }
}
