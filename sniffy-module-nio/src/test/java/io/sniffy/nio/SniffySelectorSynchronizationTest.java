package io.sniffy.nio;

import org.junit.Test;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static io.sniffy.nio.NioTestSupport.*;

public class SniffySelectorSynchronizationTest {

    @Test
    public void selectionUsesSelectorThenPublicSelectedKeyMonitor() throws Exception {
        final MonitorSelector delegate = new MonitorSelector();
        final SniffySelector selector = new SniffySelector(null, delegate);
        final CountDownLatch attempted = new CountDownLatch(1);
        Thread selecting = null;
        Thread selectorContender = null;
        try {
            synchronized (selector.selectedKeys()) {
                selecting = selectionThread(selector, attempted);
                selecting.start();
                assertTrue(attempted.await(5, TimeUnit.SECONDS));
                awaitBlocked(selecting);

                final CountDownLatch selectorAttempted = new CountDownLatch(1);
                selectorContender = daemonThread("sniffy-selector-lock-contender", new Runnable() {
                    @Override public void run() {
                        selectorAttempted.countDown();
                        synchronized (selector) { }
                    }
                });
                selectorContender.start();
                assertTrue(selectorAttempted.await(5, TimeUnit.SECONDS));
                awaitBlocked(selectorContender);
            }
            joinOrDumpAndFail(selecting);
            joinOrDumpAndFail(selectorContender);
            assertTrue(delegate.entered.await(5, TimeUnit.SECONDS));
        } finally {
            joinOrDumpAndFail(selecting);
            joinOrDumpAndFail(selectorContender);
            selector.close();
        }
    }

    @Test
    public void eachPublicMonitorPreventsSelectionFromEnteringDelegate() throws Exception {
        assertMonitorBlocksSelection(true);
        assertMonitorBlocksSelection(false);
    }

    @Test
    public void closeUsesSelectorThenPublicSelectedKeyMonitor() throws Exception {
        final MonitorSelector delegate = new MonitorSelector();
        final SniffySelector selector = new SniffySelector(null, delegate);
        final CountDownLatch attempted = new CountDownLatch(1);
        Thread closing = null;
        Thread contender = null;
        try {
            synchronized (selector.selectedKeys()) {
                closing = daemonThread("sniffy-selector-close-order", new Runnable() {
                    @Override public void run() {
                        attempted.countDown();
                        try { selector.close(); } catch (IOException e) { throw new AssertionError(e); }
                    }
                });
                closing.start();
                assertTrue(attempted.await(5, TimeUnit.SECONDS));
                awaitBlocked(closing);
                contender = selectorContender(selector);
                awaitBlocked(contender);
            }
            joinOrDumpAndFail(closing);
            joinOrDumpAndFail(contender);
            assertTrue(delegate.closed.await(5, TimeUnit.SECONDS));
        } finally {
            selector.wakeup();
            joinOrDumpAndFail(closing);
            joinOrDumpAndFail(contender);
            selector.close();
        }
    }

    @Test
    public void consumerRunsOnlyUnderPublicMonitorsAfterDelegateMonitorsAreReleased() throws Exception {
        final MonitorSelector delegate = new MonitorSelector();
        final SniffySelector selector = new SniffySelector(null, delegate);
        TestKey delegateKey = new TestKey(delegate);
        SelectionKeyLink link = new SelectionKeyLink(selector, null, null);
        delegateKey.attach(link);
        final SniffySelectionKey wrapper = link.wrapper(delegateKey);
        delegate.ready = delegateKey;

        Consumer<SelectionKey> action = new Consumer<SelectionKey>() {
            @Override public void accept(SelectionKey key) {
                assertSame(wrapper, key);
                assertTrue(Thread.holdsLock(selector));
                assertTrue(Thread.holdsLock(selector.selectedKeys()));
                assertFalse(Thread.holdsLock(delegate));
                assertFalse(Thread.holdsLock(delegate.selectedKeys));
                assertFalse(SniffySelectorProvider.isDelegateSelectorConstruction());
            }
        };
        assertEquals(1, selector.selectNow(action));
        assertEquals(1, selector.select(action, 1));
        assertEquals(1, selector.select(action));
        selector.close();
    }

    @Test
    public void selectedKeySetCanBeIteratedWhileItsPublicMonitorBlocksSelection() throws Exception {
        MonitorSelector delegate = new MonitorSelector();
        SniffySelector selector = new SniffySelector(null, delegate);
        TestKey delegateKey = new TestKey(delegate);
        SelectionKeyLink link = new SelectionKeyLink(selector, null, null);
        delegateKey.attach(link);
        SniffySelectionKey wrapper = link.wrapper(delegateKey);
        delegate.ready = delegateKey;
        selector.selectNow();

        CountDownLatch attempted = new CountDownLatch(1);
        Thread selecting = null;
        try {
            synchronized (selector.selectedKeys()) {
                selecting = selectionThread(selector, attempted);
                selecting.start();
                assertTrue(attempted.await(5, TimeUnit.SECONDS));
                awaitBlocked(selecting);
                assertSame(wrapper, selector.selectedKeys().iterator().next());
            }
            joinOrDumpAndFail(selecting);
        } finally {
            joinOrDumpAndFail(selecting);
            selector.close();
        }
    }

    private static void assertMonitorBlocksSelection(boolean selectorMonitor) throws Exception {
        MonitorSelector delegate = new MonitorSelector();
        SniffySelector selector = new SniffySelector(null, delegate);
        Object monitor = selectorMonitor ? selector : selector.selectedKeys();
        CountDownLatch attempted = new CountDownLatch(1);
        Thread thread;
        thread = null;
        try {
            synchronized (monitor) {
                thread = selectionThread(selector, attempted);
                thread.start();
                assertTrue(attempted.await(5, TimeUnit.SECONDS));
                awaitBlocked(thread);
                assertEquals(1L, delegate.entered.getCount());
            }
            assertTrue(delegate.entered.await(5, TimeUnit.SECONDS));
            joinOrDumpAndFail(thread);
        } finally {
            joinOrDumpAndFail(thread);
            selector.close();
        }
    }

    private static Thread selectionThread(final SniffySelector selector, final CountDownLatch attempted) {
        return daemonThread("sniffy-selection-monitor-test", new Runnable() {
            @Override public void run() {
                attempted.countDown();
                try { selector.selectNow(); } catch (IOException e) { throw new AssertionError(e); }
            }
        });
    }

    private static Thread selectorContender(final SniffySelector selector) {
        final CountDownLatch attempted = new CountDownLatch(1);
        Thread contender = daemonThread("sniffy-selector-contender", new Runnable() {
            @Override public void run() {
                attempted.countDown();
                synchronized (selector) { }
            }
        });
        contender.start();
        try { assertTrue(attempted.await(5, TimeUnit.SECONDS)); } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new AssertionError(e);
        }
        return contender;
    }

    private static void awaitBlocked(Thread thread) {
        for (int i = 0; i < 100000; i++) {
            if (thread.getState() == Thread.State.BLOCKED) return;
            Thread.yield();
        }
        fail("thread did not block on the expected monitor; state=" + thread.getState());
    }

    private static final class MonitorSelector extends AbstractSelector {
        private final Set<SelectionKey> keys = new HashSet<SelectionKey>();
        private final Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private SelectionKey ready;

        MonitorSelector() { super(null); }
        @Override protected void implCloseSelector() { closed.countDown(); }
        @Override protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) { throw new UnsupportedOperationException(); }
        @Override public Set<SelectionKey> keys() { return keys; }
        @Override public Set<SelectionKey> selectedKeys() { return selectedKeys; }
        @Override public int selectNow() { synchronized (this) { synchronized (selectedKeys) {
            entered.countDown(); if (ready != null) selectedKeys.add(ready); return ready == null ? 0 : 1;
        } } }
        @Override public int select(long timeout) { return selectNow(); }
        @Override public int select() { return selectNow(); }
        @Override public Selector wakeup() { return this; }
    }

    private static final class TestKey extends SelectionKey {
        private final Selector selector;
        TestKey(Selector selector) { this.selector = selector; }
        @Override public SelectableChannel channel() { return null; }
        @Override public Selector selector() { return selector; }
        @Override public boolean isValid() { return true; }
        @Override public void cancel() { }
        @Override public int interestOps() { return SelectionKey.OP_READ; }
        @Override public SelectionKey interestOps(int ops) { return this; }
        @Override public int readyOps() { return SelectionKey.OP_READ; }
    }
}
