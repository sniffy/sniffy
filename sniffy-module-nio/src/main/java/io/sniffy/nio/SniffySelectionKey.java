package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ObjectWrapper;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * @since 3.1.7
 */
public class SniffySelectionKey extends AbstractSelectionKey implements ObjectWrapper<SelectionKey> {

    private final SelectionKey delegate;
    private final SniffySelector sniffySelector;
    private final SelectableChannel sniffyChannel;

    protected SniffySelectionKey(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffyChannel) {
        this(delegate, sniffySelector, sniffyChannel, null == delegate ? null : delegate.attachment());
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
    public int interestOps() {
        return delegate.interestOps();
    }

    @Override
    public SelectionKey interestOps(int ops) {
        delegate.interestOps(ops);
        return this;
    }

    @Override
    public int readyOps() {
        return delegate.readyOps();
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings("Since15")
    public int interestOpsOr(int ops) {
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
        try {
            return invokeMethod(SelectionKey.class, delegate, "interestOpsAnd", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

}
