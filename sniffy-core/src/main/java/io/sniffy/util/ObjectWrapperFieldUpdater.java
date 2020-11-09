package io.sniffy.util;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ObjectWrapperFieldUpdater<C, O> extends AtomicReferenceFieldUpdater<C, O> {

    private final AtomicReferenceFieldUpdater<C, O> defaultFieldUpdater;

    public ObjectWrapperFieldUpdater(AtomicReferenceFieldUpdater<C, O> defaultFieldUpdater) {
        this.defaultFieldUpdater = defaultFieldUpdater;
    }

    @SuppressWarnings("unchecked")
    private C getWrappedObject(C wrapper) {
        return ((ObjectWrapper<C>) wrapper).getDelegate();
    }

    @Override
    public boolean compareAndSet(C obj, O expect, O update) {
        defaultFieldUpdater.compareAndSet(obj, expect, update);
        return defaultFieldUpdater.compareAndSet(getWrappedObject(obj), expect, update);
    }

    @Override
    public boolean weakCompareAndSet(C obj, O expect, O update) {
        defaultFieldUpdater.weakCompareAndSet(obj, expect, update);
        return defaultFieldUpdater.weakCompareAndSet(getWrappedObject(obj), expect, update);
    }

    @Override
    public void set(C obj, O newValue) {
        defaultFieldUpdater.set(obj, newValue);
        defaultFieldUpdater.set(getWrappedObject(obj), newValue);
    }

    @Override
    public void lazySet(C obj, O newValue) {
        defaultFieldUpdater.lazySet(obj, newValue);
        defaultFieldUpdater.lazySet(getWrappedObject(obj), newValue);
    }

    @Override
    public O get(C obj) {
        defaultFieldUpdater.get(obj);
        return defaultFieldUpdater.get(getWrappedObject(obj));
    }
}
