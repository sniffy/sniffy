package io.sniffy.nio;

import io.sniffy.util.*;

import java.lang.reflect.InvocationTargetException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * @since 3.1.7
 */
public class SniffySelectionKey extends SelectionKey implements ObjectWrapper<SelectionKey> {

    static {

        if (JVMUtil.getVersion() < 14 /*&& !Boolean.getBoolean("io.sniffy.forceJava14Compatibility")*/) {

            try {
                AtomicReferenceFieldUpdater<SelectionKey, Object> defaultFieldUpdater = ReflectionUtil.getField(SelectionKey.class, null, "attachmentUpdater");
                AtomicReferenceFieldUpdater<SelectionKey, Object> attachmentFieldUpdater = new ObjectWrapperFieldUpdater<SelectionKey, Object>(defaultFieldUpdater);
                ReflectionUtil.setField(SelectionKey.class, null, "attachmentUpdater", attachmentFieldUpdater);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private final SelectionKey delegate;
    private final SniffySelector sniffySelector;
    private final SelectableChannel sniffyChannel;

    protected SniffySelectionKey(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffyChannel) {
        this.delegate = delegate;

        if (null != delegate) {
            attach(delegate.attachment());
        }

        this.sniffySelector = sniffySelector;
        this.sniffyChannel = sniffyChannel;
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
        return delegate.isValid();
    }

    @Override
    public void cancel() {
        synchronized (this) {
            delegate.cancel();
            try {
                // TODO: reevaluate copying other fields across NIO stack
                ReflectionUtil.invokeMethod(AbstractSelector.class, sniffySelector, "cancel", SelectionKey.class, this);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            }
        }
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
    public int interestOpsOr(int ops) {
        try {
            return invokeMethod(SelectionKey.class, delegate, "interestOpsOr", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    public int interestOpsAnd(int ops) {
        try {
            return invokeMethod(SelectionKey.class, delegate, "interestOpsAnd", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

}
