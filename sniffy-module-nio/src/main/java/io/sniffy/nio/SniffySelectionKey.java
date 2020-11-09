package io.sniffy.nio;

import io.sniffy.util.*;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static io.sniffy.util.ReflectionUtil.invokeMethod;
import static io.sniffy.util.ReflectionUtil.setField;

public class SniffySelectionKey extends SelectionKey implements ObjectWrapper<SelectionKey> {

    private final SelectionKey delegate;
    private final SniffySelector sniffySelector;
    private final SelectableChannel sniffyChannel;

    @Override
    public SelectionKey delegate() {
        return delegate;
    }

    private static Map<SelectionKey, SniffySelectionKey> sniffySelectionKeyCache = new ConcurrentHashMap<SelectionKey, SniffySelectionKey>(); // TODO: fix memory leak

    public static SniffySelectionKey wrap(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffyChannel) {
        SniffySelectionKey sniffySelectionKey = sniffySelectionKeyCache.get(delegate);
        if (null == sniffySelectionKey) {
            sniffySelectionKey = new SniffySelectionKey(delegate, sniffySelector, sniffyChannel);
            sniffySelectionKeyCache.put(delegate, sniffySelectionKey); // TODO: make thread safe
        }
        return sniffySelectionKey;
    }

    private SniffySelectionKey(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffyChannel) {
        this.delegate = delegate;

        if (null != delegate) {
            attach(delegate.attachment());
        }

        this.sniffySelector = sniffySelector;
        this.sniffyChannel = sniffyChannel;
    }

    static {

        if (JVMUtil.getVersion() < 14) {

            try {
                AtomicReferenceFieldUpdater<SelectionKey, Object> defaultFieldUpdater = ReflectionUtil.getField(SelectionKey.class, null, "attachmentUpdater");
                AtomicReferenceFieldUpdater<SelectionKey, Object> attachmentFieldUpdater = new ObjectWrapperFieldUpdater<SelectionKey, Object>(defaultFieldUpdater);
                ReflectionUtil.setField(SelectionKey.class, null, "attachmentUpdater", attachmentFieldUpdater);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    public SelectionKey getDelegate() {
        return delegate;
    }

    @Override
    public SelectableChannel channel() {
        return null == sniffyChannel ? delegate.channel() : sniffyChannel; // TODO: we shouldn't have nulls here
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
        delegate.cancel();
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

    @Override
    public String toString() {
        return "SniffySelectionKey{" +
                "delegate=" + delegate +
                ", sniffySelector=" + sniffySelector +
                ", sniffyChannel=" + sniffyChannel +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SniffySelectionKey that = (SniffySelectionKey) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
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
