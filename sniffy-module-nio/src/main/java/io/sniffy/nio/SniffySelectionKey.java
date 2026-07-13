package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ObjectWrapper;

import java.nio.channels.SelectableChannel;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * @since 3.1.7
 */
public class SniffySelectionKey extends SelectionKey implements ObjectWrapper<SelectionKey> {

    private final SelectionKey delegate;
    private final SniffySelector sniffySelector;
    private final SelectableChannel sniffyChannel;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    protected SniffySelectionKey(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffyChannel) {
        this(delegate, sniffySelector, sniffyChannel, compatibilityAttachment(delegate));
    }

    SniffySelectionKey(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffyChannel,
                       Object userAttachment) {
        this.delegate = delegate;
        this.sniffySelector = sniffySelector;
        this.sniffyChannel = sniffyChannel;
        if (userAttachment != null) {
            attach(userAttachment);
        }
    }

    @Override
    public SelectionKey getDelegate() {
        return delegate;
    }

    @Override
    public SelectableChannel channel() {
        return sniffyChannel;
    }

    @Override
    public Selector selector() {
        return sniffySelector;
    }

    @Override
    public boolean isValid() {
        return !cancelled.get()
                && delegate != null
                && delegate.isValid()
                && (sniffySelector == null || sniffySelector.isOpen())
                && (sniffyChannel == null || sniffyChannel.isOpen());
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true) && delegate != null) {
            delegate.cancel();
        }
    }

    @Override
    public int interestOps() {
        ensureValid();
        return delegate.interestOps();
    }

    @Override
    public SelectionKey interestOps(int ops) {
        ensureValid();
        delegate.interestOps(ops);
        return this;
    }

    @Override
    public int readyOps() {
        ensureValid();
        return delegate.readyOps();
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings("Since15")
    public int interestOpsOr(int ops) {
        ensureValid();
        try {
            return invokeMethod(SelectionKey.class, delegate, "interestOpsOr", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings("Since15")
    public int interestOpsAnd(int ops) {
        ensureValid();
        try {
            return invokeMethod(SelectionKey.class, delegate, "interestOpsAnd", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    private void ensureValid() {
        if (!isValid()) {
            throw new CancelledKeyException();
        }
    }

    /**
     * Keeps the historical constructor signature without ever exposing Sniffy's internal
     * delegate attachment as the application attachment of a wrapper key.
     */
    private static Object compatibilityAttachment(SelectionKey delegate) {
        if (delegate == null) {
            return null;
        }
        Object attachment = delegate.attachment();
        return attachment instanceof SelectionKeyLink ? null : attachment;
    }

}
