package io.sniffy.nio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.SelectionKey;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.Pipe;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assume.assumeTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SniffySelectorLifecycleTest {

    @Before
    public void installProvider() {
        SniffySelectorProviderModule.initialize();
    }

    @After
    public void uninstallProvider() {
        SniffySelectorProvider.uninstall();
    }

    @Test
    public void reregisterReturnsSameKeyAndReplacesUserAttachment() throws Exception {
        Selector selector = Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            Object firstAttachment = new Object();
            Object secondAttachment = new Object();
            SelectionKey first = channel.register(selector, SelectionKey.OP_CONNECT, firstAttachment);
            SelectionKey second = channel.register(selector, SelectionKey.OP_READ, secondAttachment);

            assertSame(first, second);
            assertSame(first, channel.keyFor(selector));
            assertSame(first, selector.keys().iterator().next());
            assertSame(secondAttachment, first.attachment());
            assertEquals(SelectionKey.OP_READ, first.interestOps());
            assertTrue(first.channel() instanceof SniffySocketChannel);
            assertSame(selector, first.selector());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void wrapperCancellationImmediatelyCancelsDelegate() throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            SniffySelectionKey key = (SniffySelectionKey) channel.register(selector, SelectionKey.OP_CONNECT);

            key.cancel();

            assertFalse(key.isValid());
            assertFalse(key.getDelegate().isValid());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void channelAndSelectorClosureImmediatelyInvalidateWrapper() throws Exception {
        assertImmediateInvalidation(CloseTarget.DELEGATE_CHANNEL);
        assertImmediateInvalidation(CloseTarget.WRAPPER_CHANNEL);
        assertImmediateInvalidation(CloseTarget.DELEGATE_SELECTOR);
        assertImmediateInvalidation(CloseTarget.WRAPPER_SELECTOR);
    }

    private void assertImmediateInvalidation(CloseTarget target) throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            SniffySelectionKey key = (SniffySelectionKey) channel.register(selector, SelectionKey.OP_CONNECT);
            if (target == CloseTarget.DELEGATE_CHANNEL) {
                key.getDelegate().channel().close();
            } else if (target == CloseTarget.WRAPPER_CHANNEL) {
                channel.close();
            } else if (target == CloseTarget.DELEGATE_SELECTOR) {
                key.getDelegate().selector().close();
            } else {
                selector.close();
            }
            assertFalse(target.name(), key.isValid());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void wrapperAndDelegateCancellationAreReconciledAfterSelection() throws Exception {
        assertCancellationCleaned(false);
        assertCancellationCleaned(true);
    }

    private void assertCancellationCleaned(boolean cancelDelegate) throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);
            if (cancelDelegate) {
                ((SniffySelectionKey) key).getDelegate().cancel();
            } else {
                key.cancel();
                key.cancel();
            }

            selector.selectNow();

            assertFalse(key.isValid());
            assertNull(channel.keyFor(selector));
            assertTrue(selector.keys().isEmpty());
            assertEquals(0, selector.activeLinkCount());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void channelAndDelegateClosureAreReconciledAfterSelection() throws Exception {
        assertChannelCloseCleaned(false);
        assertChannelCloseCleaned(true);
    }

    private void assertChannelCloseCleaned(boolean closeDelegate) throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);
            if (closeDelegate) {
                ((SniffySelectionKey) key).getDelegate().channel().close();
            } else {
                channel.close();
            }

            selector.selectNow();

            assertFalse(key.isValid());
            assertNull(channel.keyFor(selector));
            assertEquals(0, selector.activeLinkCount());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void selectorCloseIsIdempotentAndCleansWrapperChannel() throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);

            selector.close();
            selector.close();

            assertFalse(selector.isOpen());
            assertFalse(key.isValid());
            assertNull(channel.keyFor(selector));
            assertEquals(0, selector.activeLinkCount());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void selectorCloseRemovesAllWrapperRegistrations() throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel firstChannel = SocketChannel.open();
        SocketChannel secondChannel = SocketChannel.open();
        try {
            firstChannel.configureBlocking(false);
            secondChannel.configureBlocking(false);
            SelectionKey first = firstChannel.register(selector, SelectionKey.OP_CONNECT);
            SelectionKey second = secondChannel.register(selector, SelectionKey.OP_CONNECT);

            selector.close();

            assertFalse(first.isValid());
            assertFalse(second.isValid());
            assertNull(firstChannel.keyFor(selector));
            assertNull(secondChannel.keyFor(selector));
            assertEquals(0, selector.activeLinkCount());
        } finally {
            firstChannel.close();
            secondChannel.close();
            selector.close();
        }
    }

    @Test
    public void repeatedRegisterCancelCyclesDoNotGrowOwnershipState() throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            for (int i = 0; i < 100; i++) {
                SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);
                key.cancel();
                selector.selectNow();
                assertNull("cycle " + i, channel.keyFor(selector));
                assertEquals("cycle " + i, 0, selector.activeLinkCount());
            }
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void cancelThenConfigureBlockingTrueDoesNotRequireIntermediateSelect() throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);

            key.cancel();
            channel.configureBlocking(true);

            assertTrue(channel.isBlocking());
            assertFalse(((SniffySelectionKey) key).getDelegate().isValid());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void cancelThenReregisterBeforeDeregistrationFollowsJdkContract() throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            SelectionKey cancelled = channel.register(selector, SelectionKey.OP_CONNECT);
            cancelled.cancel();

            try {
                channel.register(selector, SelectionKey.OP_READ);
                fail("The cancelled key remains registered until selection processes cancellation");
            } catch (CancelledKeyException expected) {
                // expected JDK behavior
            }

            selector.selectNow();
            SelectionKey replacement = channel.register(selector, SelectionKey.OP_READ);
            assertTrue(replacement.isValid());
            assertFalse(replacement == cancelled);
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void reregistrationDoesNotRetainOldAttachmentInLink() throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            Object oldAttachment = new Object();
            Object replacement = new Object();
            SniffySelectionKey key = (SniffySelectionKey) channel.register(
                    selector, SelectionKey.OP_CONNECT, oldAttachment);
            SelectionKeyLink link = (SelectionKeyLink) key.getDelegate().attachment();

            channel.register(selector, SelectionKey.OP_READ, replacement);

            java.lang.reflect.Field initialAttachment = SelectionKeyLink.class
                    .getDeclaredField("initialUserAttachment");
            initialAttachment.setAccessible(true);
            assertNull(initialAttachment.get(link));
            assertSame(replacement, key.attachment());
            assertSame(link, key.getDelegate().attachment());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void closingSocketViewClosesWrapperChannelAndCleansKey() throws Exception {
        SniffySelector selector = (SniffySelector) Selector.open();
        SniffySocketChannel channel = (SniffySocketChannel) SocketChannel.open();
        try {
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);

            channel.socket().close();
            selector.selectNow();

            assertFalse(channel.isOpen());
            assertFalse(channel.getDelegate().isOpen());
            assertFalse(key.isValid());
            assertNull(channel.keyFor(selector));
            assertEquals(0, selector.activeLinkCount());
        } finally {
            channel.close();
            selector.close();
        }
    }

    @Test
    public void consumerSelectVariantsExposeWrappersAndProcessCancellation() throws Exception {
        assumeTrue(runtimeFeatureVersion() >= 11);
        SniffySelector selector = (SniffySelector) Selector.open();
        Pipe pipe = Pipe.open();
        try {
            pipe.source().configureBlocking(false);
            final SelectionKey key = pipe.source().register(selector, SelectionKey.OP_READ);
            final AtomicReference<SelectionKey> consumed = new AtomicReference<SelectionKey>();
            Consumer<SelectionKey> consumer = new Consumer<SelectionKey>() {
                @Override
                public void accept(SelectionKey selected) {
                    consumed.set(selected);
                }
            };

            pipe.sink().write(ByteBuffer.wrap(new byte[]{1}));
            assertEquals(1, selector.selectNow(consumer));
            assertSame(key, consumed.get());
            pipe.source().read(ByteBuffer.allocate(1));

            consumed.set(null);
            pipe.sink().write(ByteBuffer.wrap(new byte[]{2}));
            assertEquals(1, selector.select(consumer, 1000));
            assertSame(key, consumed.get());
            pipe.source().read(ByteBuffer.allocate(1));

            consumed.set(null);
            pipe.sink().write(ByteBuffer.wrap(new byte[]{3}));
            assertEquals(1, selector.select(consumer));
            assertSame(key, consumed.get());

            key.cancel();
            selector.selectNow(consumer);
            assertNull(pipe.source().keyFor(selector));
            assertEquals(0, selector.activeLinkCount());
        } finally {
            pipe.source().close();
            pipe.sink().close();
            selector.close();
        }
    }

    @Test
    public void consumerSelectDoesNotLeakDelegateConstructionScopeToApplicationConsumer() throws Exception {
        assumeTrue(runtimeFeatureVersion() >= 11);
        SniffySelector selector = (SniffySelector) Selector.open();
        Pipe pipe = Pipe.open();
        final AtomicReference<SocketChannel> channelOpenedByConsumer = new AtomicReference<SocketChannel>();
        try {
            pipe.source().configureBlocking(false);
            pipe.source().register(selector, SelectionKey.OP_READ);
            Consumer<SelectionKey> consumer = new Consumer<SelectionKey>() {
                @Override
                public void accept(SelectionKey selected) {
                    try {
                        channelOpenedByConsumer.set(SocketChannel.open());
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
            };

            pipe.sink().write(ByteBuffer.wrap(new byte[]{1}));
            assertEquals(1, selector.selectNow(consumer));
            assertTrue(channelOpenedByConsumer.get() instanceof SniffySocketChannel);
        } finally {
            SocketChannel channel = channelOpenedByConsumer.get();
            if (channel != null) {
                channel.close();
            }
            pipe.source().close();
            pipe.sink().close();
            selector.close();
        }
    }

    @Test
    public void cleanupRunsWhenSelectionConsumerThrows() throws Exception {
        assumeTrue(runtimeFeatureVersion() >= 11);
        SniffySelector selector = (SniffySelector) Selector.open();
        Pipe pipe = Pipe.open();
        final RuntimeException primary = new RuntimeException("consumer failure");
        try {
            pipe.source().configureBlocking(false);
            final SelectionKey key = pipe.source().register(selector, SelectionKey.OP_READ);
            pipe.sink().write(ByteBuffer.wrap(new byte[]{1}));

            try {
                selector.selectNow(new Consumer<SelectionKey>() {
                    @Override
                    public void accept(SelectionKey selected) {
                        assertSame(key, selected);
                        selected.cancel();
                        throw primary;
                    }
                });
                fail("Expected the consumer failure");
            } catch (RuntimeException e) {
                assertSame(primary, e);
            }

            assertNull(pipe.source().keyFor(selector));
            assertEquals(0, selector.activeLinkCount());
        } finally {
            pipe.source().close();
            pipe.sink().close();
            selector.close();
        }
    }

    @Test
    public void concurrentRegisterSelectCancelAndCloseDoNotDeadlock() throws Exception {
        assertConcurrentSelectLifecycle(LifecycleAction.REGISTER);
        assertConcurrentSelectLifecycle(LifecycleAction.CANCEL);
        assertConcurrentSelectLifecycle(LifecycleAction.CLOSE);
    }

    private void assertConcurrentSelectLifecycle(LifecycleAction action) throws Exception {
        final SniffySelector selector = (SniffySelector) Selector.open();
        final SocketChannel channel = SocketChannel.open();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final AtomicBoolean actionCompleted = new AtomicBoolean();
        final CountDownLatch selecting = new CountDownLatch(1);
        channel.configureBlocking(false);
        final SelectionKey existing = action == LifecycleAction.REGISTER ? null :
                channel.register(selector, SelectionKey.OP_CONNECT);
        Thread selectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    selecting.countDown();
                    do {
                        selector.select();
                    } while (!actionCompleted.get());
                } catch (Throwable e) {
                    failure.set(e);
                }
            }
        }, "sniffy-concurrent-select-" + action);
        selectThread.start();
        assertTrue(selecting.await(5, TimeUnit.SECONDS));
        try {
            if (action == LifecycleAction.REGISTER) {
                // Registration may wait for an in-progress selection; wakeup is the JDK-prescribed handoff.
                selector.wakeup();
                channel.register(selector, SelectionKey.OP_CONNECT);
            } else if (action == LifecycleAction.CANCEL) {
                existing.cancel();
            } else {
                channel.close();
            }
            actionCompleted.set(true);
            selector.wakeup();
            selectThread.join(5000);
            assertFalse(selectThread.isAlive());
            assertNull(failure.get());
            if (action != LifecycleAction.REGISTER) {
                assertNull("the returning selection operation must reconcile wrapper registration",
                        channel.keyFor(selector));
                assertEquals(0, selector.activeLinkCount());
            }
        } finally {
            channel.close();
            selector.close();
        }
    }

    private enum LifecycleAction {
        REGISTER, CANCEL, CLOSE
    }

    private enum CloseTarget {
        DELEGATE_CHANNEL, WRAPPER_CHANNEL, DELEGATE_SELECTOR, WRAPPER_SELECTOR
    }

    private static int runtimeFeatureVersion() {
        String version = System.getProperty("java.specification.version", "8");
        if (version.startsWith("1.")) version = version.substring(2);
        int dot = version.indexOf('.');
        return Integer.parseInt(dot < 0 ? version : version.substring(0, dot));
    }
}
