package io.sniffy.nio;

import org.junit.Test;

import java.nio.channels.SelectableChannel;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.nio.NioTestSupport.daemonThread;
import static io.sniffy.nio.NioTestSupport.dumpThreads;
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
    public void wrapperExtendsSelectionKeyWithoutAbstractSelectorCancellationLeak() {
        assertSame(SelectionKey.class, SniffySelectionKey.class.getSuperclass());
        assertFalse(AbstractSelectionKey.class.isAssignableFrom(SniffySelectionKey.class));

        TestSelectionKey delegate = new TestSelectionKey();
        InspectingSniffySelector selector = new InspectingSniffySelector(new TestSelector());
        SniffySelectionKey key = new SniffySelectionKey(delegate, selector, null);
        key.cancel();
        assertEquals(0, selector.cancelledKeyCount());
    }

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
    public void compatibilityConstructorDoesNotExposeInternalLinkAsUserAttachment() {
        TestSelectionKey delegate = new TestSelectionKey();
        SelectionKeyLink link = new SelectionKeyLink(null, null, new Object());
        delegate.attach(link);

        SniffySelectionKey key = new SniffySelectionKey(delegate, null, null);

        assertNull(key.attachment());
        assertSame(link, delegate.attachment());
    }

    @Test
    public void wrapperPublicationIsSafeAndUnique() throws Exception {
        final TestSelectionKey delegate = new TestSelectionKey();
        final SniffySelector selector = new SniffySelector(null, null);
        final TestChannel channel = new TestChannel();
        delegate.attach(new SelectionKeyLink(selector, channel, null));
        final int taskCount = 16;
        final CountDownLatch ready = new CountDownLatch(taskCount);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger threadId = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(taskCount, new ThreadFactory() {
            @Override public Thread newThread(Runnable task) {
                return daemonThread("sniffy-key-publication-" + threadId.incrementAndGet(), task);
            }
        });
        try {
            List<Future<SniffySelectionKey>> futures = new ArrayList<Future<SniffySelectionKey>>();
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(new Callable<SniffySelectionKey>() {
                    @Override
                    public SniffySelectionKey call() throws Exception {
                        ready.countDown();
                        start.await();
                        SniffySelectionKey key = selector.wrap(delegate, selector, channel);
                        assertSame(delegate, key.getDelegate());
                        assertSame(selector, key.selector());
                        assertSame(channel, key.channel());
                        return key;
                    }
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            SniffySelectionKey expected = futures.get(0).get(5, TimeUnit.SECONDS);
            for (Future<SniffySelectionKey> future : futures) {
                assertSame(expected, future.get(5, TimeUnit.SECONDS));
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                dumpThreads("selection-key publication executor did not terminate");
                throw new AssertionError("Selection-key publication executor did not terminate");
            }
            channel.close();
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
        final AtomicReference<SelectionKey> forEachKey = new AtomicReference<SelectionKey>();
        keys.forEach(forEachKey::set);
        assertSame(key, forEachKey.get());
        final AtomicReference<SelectionKey> spliteratorKey = new AtomicReference<SelectionKey>();
        assertTrue(keys.spliterator().tryAdvance(spliteratorKey::set));
        assertSame(key, spliteratorKey.get());

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
        assertFalse(selectedKeys.retainAll(Collections.<SelectionKey>singleton(key)));
        assertTrue(selectedKeys.retainAll(Collections.<SelectionKey>emptySet()));
        delegateSelector.selectedKeys.add(delegateKey);
        java.util.Iterator<SelectionKey> removableIterator = selectedKeys.iterator();
        removableIterator.next();
        removableIterator.remove();
        assertTrue(selectedKeys.isEmpty());
        delegateSelector.selectedKeys.add(delegateKey);
        selectedKeys.clear();
        assertTrue(selectedKeys.isEmpty());
    }

    @Test
    public void keysViewAlwaysRejectsEveryMutationMethod() {
        final KeySetFixture fixture = new KeySetFixture();
        final Set<SelectionKey> keys = fixture.selector.keys();

        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                keys.add(fixture.key);
            }
        });
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                keys.addAll(Collections.<SelectionKey>emptySet());
            }
        });
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                keys.remove(new Object());
            }
        });
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                keys.removeAll(Collections.emptySet());
            }
        });
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                keys.retainAll(Collections.singleton(fixture.key));
            }
        });
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                keys.removeIf(key -> false);
            }
        });
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                keys.clear();
            }
        });
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                java.util.Iterator<SelectionKey> iterator = keys.iterator();
                iterator.next();
                iterator.remove();
            }
        });
        assertTrue(keys.contains(fixture.key));
    }

    @Test
    public void selectedKeysRejectsAddAndAddAll() {
        final KeySetFixture fixture = new KeySetFixture();
        final Set<SelectionKey> selectedKeys = fixture.selector.selectedKeys();

        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                selectedKeys.add(fixture.key);
            }
        });
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                selectedKeys.addAll(Collections.<SelectionKey>emptySet());
            }
        });
    }

    @Test
    public void selectedKeysSupportsAllRemovalMethods() {
        KeySetFixture fixture = new KeySetFixture();
        Set<SelectionKey> selectedKeys = fixture.selector.selectedKeys();

        assertTrue(selectedKeys.remove(fixture.key));
        fixture.delegateSelector.selectedKeys.add(fixture.delegateKey);
        assertTrue(selectedKeys.removeAll(Collections.<SelectionKey>singleton(fixture.key)));
        fixture.delegateSelector.selectedKeys.add(fixture.delegateKey);
        assertTrue(selectedKeys.retainAll(Collections.emptySet()));
        fixture.delegateSelector.selectedKeys.add(fixture.delegateKey);
        assertTrue(selectedKeys.removeIf(key -> key == fixture.key));
        fixture.delegateSelector.selectedKeys.add(fixture.delegateKey);
        java.util.Iterator<SelectionKey> iterator = selectedKeys.iterator();
        iterator.next();
        iterator.remove();
        fixture.delegateSelector.selectedKeys.add(fixture.delegateKey);
        selectedKeys.clear();

        assertTrue(selectedKeys.isEmpty());
    }

    @Test
    public void selectedKeysDoesNotRemoveCanonicalKeyUsingForeignWrapper() {
        KeySetFixture fixture = new KeySetFixture();
        SniffySelectionKey foreign = new SniffySelectionKey(
                fixture.delegateKey, fixture.selector, fixture.key.channel());

        assertFalse(fixture.selector.selectedKeys().contains(foreign));
        assertFalse(fixture.selector.selectedKeys().remove(foreign));
        assertTrue(fixture.selector.selectedKeys().contains(fixture.key));
    }

    private static void assertUnsupported(Runnable operation) {
        try {
            operation.run();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
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

    @Test
    public void wrapperCancellationIsImmediateAndIdempotent() {
        TestSelectionKey delegate = new TestSelectionKey();
        SniffySelector selector = new SniffySelector(null, new TestSelector());
        SniffySelectionKey key = new SniffySelectionKey(delegate, selector, null);

        key.cancel();
        key.cancel();

        assertFalse(key.isValid());
        assertEquals(1, delegate.cancellationCount);
    }

    @Test
    public void directDelegateCancellationInvalidatesWrapper() {
        TestSelectionKey delegate = new TestSelectionKey();
        SniffySelectionKey key = new SniffySelectionKey(delegate, null, null);

        delegate.cancel();

        assertFalse(key.isValid());
    }

    @Test
    public void cancelledKeyOperationsThrowCancelledKeyException() {
        final TestSelectionKey delegate = new TestSelectionKey();
        final SniffySelectionKey key = new SniffySelectionKey(delegate, null, null);
        key.cancel();

        assertCancelled(new Runnable() {
            @Override
            public void run() {
                key.interestOps();
            }
        });
        assertCancelled(new Runnable() {
            @Override
            public void run() {
                key.interestOps(SelectionKey.OP_READ);
            }
        });
        assertCancelled(new Runnable() {
            @Override
            public void run() {
                key.readyOps();
            }
        });
        if (runtimeFeatureVersion() >= 11) {
            assertCancelled(new Runnable() {
                @Override
                public void run() {
                    key.interestOpsOr(SelectionKey.OP_READ);
                }
            });
            assertCancelled(new Runnable() {
                @Override
                public void run() {
                    key.interestOpsAnd(SelectionKey.OP_READ);
                }
            });
        }
    }

    @Test
    public void selectorAlwaysReturnsRealSniffySelectorAfterCancellation() {
        TestSelectionKey delegate = new TestSelectionKey();
        SniffySelector selector = new SniffySelector(null, new TestSelector());
        SniffySelectionKey key = new SniffySelectionKey(delegate, selector, null);

        key.cancel();

        assertSame(selector, key.selector());
    }

    private static void assertCancelled(Runnable operation) {
        try {
            operation.run();
            fail("Expected CancelledKeyException");
        } catch (CancelledKeyException expected) {
            // expected
        }
    }

    private static int runtimeFeatureVersion() {
        String version = System.getProperty("java.specification.version", "8");
        int dot = version.indexOf('.');
        return Integer.parseInt(dot < 0 ? version : version.substring(dot + 1));
    }

    @Test
    public void repeatedCancellationIsIdempotent() {
        TestSelectionKey delegate = new TestSelectionKey();
        SniffySelector selector = new SniffySelector(null, new TestSelector());
        SniffySelectionKey key = new SniffySelectionKey(delegate, selector, null);

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

    private static final class KeySetFixture {
        private final TestSelector delegateSelector = new TestSelector();
        private final SniffySelector selector = new SniffySelector(null, delegateSelector);
        private final TestSelectionKey delegateKey = new TestSelectionKey();
        private final SniffySelectionKey key;

        private KeySetFixture() {
            SelectionKeyLink link = new SelectionKeyLink(selector, null, null);
            delegateKey.attach(link);
            key = link.wrapper(delegateKey);
            delegateSelector.keys.add(delegateKey);
            delegateSelector.selectedKeys.add(delegateKey);
        }
    }

    private static final class InspectingSniffySelector extends SniffySelector {
        private InspectingSniffySelector(AbstractSelector delegate) {
            super(null, delegate);
        }

        private int cancelledKeyCount() {
            return cancelledKeys().size();
        }
    }

    private static final class TestChannel extends AbstractSelectableChannel {
        private TestChannel() {
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
}
