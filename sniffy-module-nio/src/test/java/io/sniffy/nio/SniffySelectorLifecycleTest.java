package io.sniffy.nio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
}
