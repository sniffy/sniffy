package io.sniffy.nio;

import org.junit.Test;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import static org.junit.Assume.assumeTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

        SniffySelectionKey key = new SniffySelectionKey(delegate, null, null);

        assertSame(initialAttachment, key.attachment());
        assertSame(initialAttachment, delegate.attachment());
        assertSame(initialAttachment, key.attach(replacementAttachment));
        assertSame(replacementAttachment, key.attachment());
        assertSame("the delegate attachment is not application attachment storage",
                initialAttachment, delegate.attachment());
    }

    @Test
    public void selectorPublishesOneWrapperForADelegateKey() {
        TestSelectionKey delegate = new TestSelectionKey();
        SniffySelector selector = new SniffySelector(null, null);

        SniffySelectionKey first = selector.wrap(delegate, selector, null);
        SniffySelectionKey second = selector.wrap(delegate, selector, null);

        assertSame(first, second);
        assertSame(delegate, first.getDelegate());
        assertSame(selector, first.selector());
        assertNull(first.channel());
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
}
