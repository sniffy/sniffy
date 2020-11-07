package io.sniffy.nio;

import io.sniffy.util.ReflectionCopier;
import io.sniffy.util.ReflectionUtil;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class SniffySelectionKey extends SelectionKey {

    private final SelectionKey delegate;
    private final SniffySelector sniffySelector;

    public SniffySelectionKey(SelectionKey delegate, SniffySelector sniffySelector) {
        this.delegate = delegate;
        //ReflectionUtil.setField(SelectionKey.class, this, "attachment", delegate.attachment());
        attach(delegate.attachment());
        this.sniffySelector = sniffySelector;
    }

    static {

        try {
            AtomicReferenceFieldUpdater<SelectionKey,Object> defaultFieldUpdater = ReflectionUtil.getField(SelectionKey.class, null, "attachmentUpdater");
            AtomicReferenceFieldUpdater<SelectionKey,Object> attachmentFieldUpdater = new AttachmentFieldUpdater(defaultFieldUpdater);
            ReflectionUtil.setField(SelectionKey.class, null, "attachmentUpdater", attachmentFieldUpdater);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    public SelectionKey getDelegate() {
        return delegate;
    }

    private static class AttachmentFieldUpdater extends AtomicReferenceFieldUpdater<SelectionKey,Object> {

        private final AtomicReferenceFieldUpdater<SelectionKey,Object> defaultFieldUpdater;

        public AttachmentFieldUpdater(AtomicReferenceFieldUpdater<SelectionKey, Object> defaultFieldUpdater) {
            this.defaultFieldUpdater = defaultFieldUpdater;
        }

        private SelectionKey getDelegate(SelectionKey obj) {

            if (obj instanceof SniffySelectionKey) {
                return ((SniffySelectionKey) obj).getDelegate();
            }

            return obj;

        }

        @Override
        public boolean compareAndSet(@SuppressWarnings("NullableProblems") SelectionKey obj, Object expect, Object update) {
            defaultFieldUpdater.compareAndSet(obj, expect, update);
            return defaultFieldUpdater.compareAndSet(getDelegate(obj), expect, update);
        }

        @Override
        public boolean weakCompareAndSet(@SuppressWarnings("NullableProblems") SelectionKey obj, Object expect, Object update) {
            defaultFieldUpdater.weakCompareAndSet(obj, expect, update);
            return defaultFieldUpdater.weakCompareAndSet(getDelegate(obj), expect, update);
        }

        @Override
        public void set(@SuppressWarnings("NullableProblems") SelectionKey obj, Object newValue) {
            defaultFieldUpdater.set(obj, newValue);
            defaultFieldUpdater.set(getDelegate(obj), newValue);
        }

        @Override
        public void lazySet(@SuppressWarnings("NullableProblems") SelectionKey obj, Object newValue) {
            defaultFieldUpdater.lazySet(obj, newValue);
            defaultFieldUpdater.lazySet(getDelegate(obj), newValue);
        }

        @Override
        public Object get(@SuppressWarnings("NullableProblems") SelectionKey obj) {
            defaultFieldUpdater.get(obj);
            return defaultFieldUpdater.get(getDelegate(obj));
        }

        @Override
        public Object getAndSet(@SuppressWarnings("NullableProblems") SelectionKey obj, Object newValue) {
            defaultFieldUpdater.getAndSet(obj, newValue);
            return defaultFieldUpdater.getAndSet(getDelegate(obj), newValue);
        }

    }

    @Override
    public SelectableChannel channel() {
        return delegate.channel();
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
}
