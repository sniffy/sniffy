package io.sniffy.nio;

import org.junit.Test;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assume.assumeTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Characterizes the public SelectionKey contract that the NIO rewrite must preserve.
 */
public class SniffySelectionKeyContractTest {

    @Test
    public void wrapperKeepsUserAttachmentSeparateFromDelegateAttachment() {
        TestSelectionKey delegate = new TestSelectionKey();
        Object initialAttachment = new Object();
        Object replacementAttachment = new Object();
        delegate.attach(initialAttachment);

        Object internalLink = new SelectionKeyLink(null, null, initialAttachment);
        delegate.attach(internalLink);
        SniffySelectionKey key = ((SelectionKeyLink) internalLink).wrapper(delegate);

        assertSame(initialAttachment, key.attachment());
        assertSame(internalLink, delegate.attachment());
        assertSame(initialAttachment, key.attach(replacementAttachment));
        assertSame(replacementAttachment, key.attachment());
        assertSame("the delegate attachment is not application attachment storage",
                internalLink, delegate.attachment());
    }

    @Test
    public void selectorPublishesOneWrapperForADelegateKey() {
        TestSelectionKey delegate = new TestSelectionKey();
        SniffySelector selector = new SniffySelector(null, null);
        SelectionKeyLink link = new SelectionKeyLink(selector, null, null);
        delegate.attach(link);

        SniffySelectionKey first = selector.wrap(delegate, selector, null);
        SniffySelectionKey second = selector.wrap(delegate, selector, null);

        assertSame(first, second);
        assertSame(delegate, first.getDelegate());
        assertSame(selector, first.selector());
        assertNull(first.channel());
    }

    @Test
    public void wrapperPublicationIsSafeAndUnique() throws Exception {
        final TestSelectionKey delegate = new TestSelectionKey();
        final SniffySelector selector = new SniffySelector(null, null);
        delegate.attach(new SelectionKeyLink(selector, null, null));
        final int taskCount = 16;
        final CountDownLatch ready = new CountDownLatch(taskCount);
        final CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        try {
            List<Future<SniffySelectionKey>> futures = new ArrayList<Future<SniffySelectionKey>>();
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(new Callable<SniffySelectionKey>() {
                    @Override
                    public SniffySelectionKey call() throws Exception {
                        ready.countDown();
                        start.await();
                        return selector.wrap(delegate, selector, null);
                    }
                }));
            }
            ready.await();
            start.countDown();
            SniffySelectionKey expected = futures.get(0).get();
            for (Future<SniffySelectionKey> future : futures) {
                assertSame(expected, future.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void corruptedDelegateAttachmentProducesControlledFailure() {
        TestSelectionKey delegate = new TestSelectionKey();
        SniffySelector selector = new SniffySelector(null, null);
        delegate.attach("application replaced the internal link");

        try {
            selector.wrap(delegate, selector, null);
            fail("Expected a controlled association failure");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("internal link"));
        }
    }

    @Test
    public void keySetViewsExposeOnlyStableWrappersAndEnforceMutationRules() {
        TestSelector delegateSelector = new TestSelector();
        SniffySelector selector = new SniffySelector(null, delegateSelector);
        TestSelectionKey delegateKey = new TestSelectionKey();
        SelectionKeyLink link = new SelectionKeyLink(selector, null, null);
        delegateKey.attach(link);
        SniffySelectionKey key = link.wrapper(delegateKey);
        delegateSelector.keys.add(delegateKey);
        delegateSelector.selectedKeys.add(delegateKey);

        Set<SelectionKey> keys = selector.keys();
        assertSame(keys, selector.keys());
        assertTrue(keys.contains(key));
        assertFalse(keys.contains(delegateKey));
        assertTrue(keys.containsAll(Collections.<SelectionKey>singleton(key)));
        assertSame(key, keys.iterator().next());
        assertSame(key, keys.toArray()[0]);
        assertSame(key, keys.toArray(new SelectionKey[0])[0]);

        try {
            keys.remove(key);
            fail("Selector.keys() must not support removal");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        assertSame(selectedKeys, selector.selectedKeys());
        assertTrue(selectedKeys.contains(key));
        assertTrue(selectedKeys.remove(key));
        assertTrue(selectedKeys.isEmpty());

        delegateSelector.selectedKeys.add(delegateKey);
        assertTrue(selectedKeys.removeAll(Arrays.<SelectionKey>asList(key)));
        delegateSelector.selectedKeys.add(delegateKey);
        selectedKeys.clear();
        assertTrue(selectedKeys.isEmpty());
    }

    @Test
    public void interestOperationMutatorsFollowJdkReturnContracts() {
        assumeTrue("interestOpsOr/And were added in Java 11", runtimeFeatureVersion() >= 11);
        TestSelectionKey delegate = new TestSelectionKey();
        delegate.interestOps(SelectionKey.OP_READ);
        SniffySelectionKey key = new SniffySelectionKey(delegate, null, null);

        assertSame(key, key.interestOps(SelectionKey.OP_WRITE));
        assertEquals(SelectionKey.OP_WRITE, key.interestOps());
        assertEquals(SelectionKey.OP_WRITE, key.interestOpsOr(SelectionKey.OP_CONNECT));
        assertEquals(SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT, key.interestOps());
        assertEquals(SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT,
                key.interestOpsAnd(SelectionKey.OP_CONNECT));
        assertEquals(SelectionKey.OP_CONNECT, key.interestOps());
    }

    private static int runtimeFeatureVersion() {
        String version = System.getProperty("java.specification.version", "8");
        int dot = version.indexOf('.');
        return Integer.parseInt(dot < 0 ? version : version.substring(dot + 1));
    }

    @Test
    public void repeatedCancellationIsIdempotent() {
        TestSelectionKey delegate = new TestSelectionKey();
        SniffySelectionKey key = new SniffySelectionKey(delegate, null, null);

        assertTrue(key.isValid());
        key.cancel();
        key.cancel();

        assertFalse(key.isValid());
        assertEquals(1, delegate.cancellationCount);
    }

    private static final class TestSelectionKey extends SelectionKey {

        private boolean valid = true;
        private int interestOps;
        private int cancellationCount;

        @Override
        public SelectableChannel channel() {
            return null;
        }

        @Override
        public Selector selector() {
            return null;
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        @Override
        public void cancel() {
            if (valid) {
                valid = false;
                cancellationCount++;
            }
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

    private static final class TestSelector extends AbstractSelector {

        private final Set<SelectionKey> keys = new HashSet<SelectionKey>();
        private final Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();

        private TestSelector() {
            super((SelectorProvider) null);
        }

        @Override
        protected void implCloseSelector() {
        }

        @Override
        protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
            throw new UnsupportedOperationException();
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
            return 0;
        }

        @Override
        public int select(long timeout) {
            return 0;
        }

        @Override
        public int select() {
            return 0;
        }

        @Override
        public Selector wakeup() {
            return this;
        }
    }
}
