package io.sniffy.nio;

import org.junit.Test;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SniffySelectorFailureLifecycleTest {

    @BeforeClass
    public static void initializeNioAccess() throws Exception {
        JdkNioAccess.resolve();
    }

    @Test
    public void checkedRegistrationFailureDoesNotRetainOwnership() throws Exception {
        TestDelegateSelector delegateSelector = new TestDelegateSelector();
        SniffySelector selector = new SniffySelector(null, delegateSelector);
        TestWrapperChannel channel = new TestWrapperChannel();
        channel.configureBlocking(false);
        channel.delegate.close();
        try {
            try {
                channel.register(selector, SelectionKey.OP_READ);
                fail("Expected ClosedChannelException");
            } catch (ClosedChannelException expected) {
                // expected checked registration failure
            }

            assertEquals(0, selector.activeLinkCount());
            assertEquals(0, channel.linkCount());
            assertNull(channel.keyFor(selector));
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void cleanupRunsWhenDelegateSelectThrows() throws Exception {
        TestDelegateSelector delegateSelector = new TestDelegateSelector();
        RuntimeException primary = new RuntimeException("delegate select failure");
        delegateSelector.selectFailure = primary;
        SniffySelector selector = new SniffySelector(null, delegateSelector);
        TestWrapperChannel channel = new TestWrapperChannel();
        channel.configureBlocking(false);
        try {
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
            key.cancel();

            try {
                selector.selectNow();
                fail("Expected delegate selection failure");
            } catch (RuntimeException e) {
                assertSame(primary, e);
            }

            assertNull(channel.keyFor(selector));
            assertEquals(0, channel.linkCount());
            assertEquals(0, selector.activeLinkCount());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void cleanupFailureIsSuppressedOntoPrimaryFailureAndRetried() throws Exception {
        TestDelegateSelector delegateSelector = new TestDelegateSelector();
        RuntimeException primary = new RuntimeException("delegate select failure");
        delegateSelector.selectFailure = primary;
        SniffySelector selector = new SniffySelector(null, delegateSelector);
        TestWrapperChannel channel = new TestWrapperChannel();
        channel.configureBlocking(false);
        try {
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
            key.cancel();
            channel.failUnregister = true;

            try {
                selector.selectNow();
                fail("Expected delegate selection failure");
            } catch (RuntimeException e) {
                assertSame(primary, e);
                assertEquals(1, e.getSuppressed().length);
                assertTrue(e.getSuppressed()[0] instanceof IOException);
            }

            assertEquals(1, selector.activeLinkCount());
            assertEquals(1, channel.linkCount());
            assertNull(channel.keyFor(selector));

            channel.failUnregister = false;
            delegateSelector.selectFailure = null;
            selector.selectNow();
            assertEquals(0, selector.activeLinkCount());
            assertEquals(0, channel.linkCount());
        } finally {
            channel.failUnregister = false;
            channel.close();
            selector.close();
        }
    }

    @Test
    public void selectorCloseClosesDelegateExactlyOnce() throws Exception {
        TestDelegateSelector delegateSelector = new TestDelegateSelector();
        SniffySelector selector = new SniffySelector(null, delegateSelector);

        selector.close();
        selector.close();

        assertEquals(1, delegateSelector.closeCount);
        assertFalse(selector.isOpen());
    }

    private static final class TestWrapperChannel extends AbstractSelectableChannel
            implements SelectableChannelWrapper<TestDelegateChannel> {

        private final TestDelegateChannel delegate = new TestDelegateChannel();
        private final Set<SelectionKeyLink> links = Collections.newSetFromMap(
                new ConcurrentHashMap<SelectionKeyLink, Boolean>());
        private volatile boolean failUnregister;

        private TestWrapperChannel() {
            super((SelectorProvider) null);
        }

        @Override
        public TestDelegateChannel getDelegate() {
            return delegate;
        }

        @Override
        public void registerKeyLink(SelectionKeyLink link) {
            links.add(link);
        }

        @Override
        public void unregisterKeyLink(SelectionKeyLink link) {
            if (failUnregister) {
                throw new IllegalStateException("injected channel-link cleanup failure");
            }
            links.remove(link);
        }

        private int linkCount() {
            return links.size();
        }

        @Override
        public int validOps() {
            return SelectionKey.OP_READ;
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {
            delegate.close();
        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {
            delegate.configureBlocking(block);
        }
    }

    private static final class TestDelegateChannel extends AbstractSelectableChannel {

        private TestDelegateChannel() {
            super((SelectorProvider) null);
        }

        @Override
        public int validOps() {
            return SelectionKey.OP_READ;
        }

        @Override
        protected void implCloseSelectableChannel() {
        }

        @Override
        protected void implConfigureBlocking(boolean block) {
        }
    }

    private static final class TestDelegateSelector extends AbstractSelector {

        private final Set<SelectionKey> keys = new HashSet<SelectionKey>();
        private final Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();
        private volatile RuntimeException selectFailure;
        private int closeCount;

        private TestDelegateSelector() {
            super((SelectorProvider) null);
        }

        @Override
        protected void implCloseSelector() {
            closeCount++;
            for (SelectionKey key : keys) {
                key.cancel();
            }
        }

        @Override
        protected SelectionKey register(AbstractSelectableChannel channel, int ops, Object attachment) {
            TestDelegateKey key = new TestDelegateKey(channel, this, ops);
            key.attach(attachment);
            keys.add(key);
            return key;
        }

        @Override
        public Set<SelectionKey> keys() {
            return keys;
        }

        @Override
        public Set<SelectionKey> selectedKeys() {
            return selectedKeys;
        }

        @Override
        public int selectNow() {
            if (selectFailure != null) {
                throw selectFailure;
            }
            return 0;
        }

        @Override
        public int select(long timeout) {
            return selectNow();
        }

        @Override
        public int select() {
            return selectNow();
        }

        @Override
        public Selector wakeup() {
            return this;
        }
    }

    private static final class TestDelegateKey extends SelectionKey {

        private final SelectableChannel channel;
        private final Selector selector;
        private volatile boolean valid = true;
        private volatile int interestOps;

        private TestDelegateKey(SelectableChannel channel, Selector selector, int interestOps) {
            this.channel = channel;
            this.selector = selector;
            this.interestOps = interestOps;
        }

        @Override
        public SelectableChannel channel() {
            return channel;
        }

        @Override
        public Selector selector() {
            return selector;
        }

        @Override
        public boolean isValid() {
            return valid && channel.isOpen() && selector.isOpen();
        }

        @Override
        public void cancel() {
            valid = false;
        }

        @Override
        public int interestOps() {
            return interestOps;
        }

        @Override
        public SelectionKey interestOps(int ops) {
            interestOps = ops;
            return this;
        }

        @Override
        public int readyOps() {
            return 0;
        }
    }
}
