package io.sniffy.util;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @since 3.1.7
 */
public class ObjectWrapperFieldUpdater<C, O> extends AtomicReferenceFieldUpdater<C, O> {

    private final AtomicReferenceFieldUpdater<C, O> defaultFieldUpdater;

    public ObjectWrapperFieldUpdater(AtomicReferenceFieldUpdater<C, O> defaultFieldUpdater) {
        this.defaultFieldUpdater = defaultFieldUpdater;
    }

    private boolean isWrappedObject(C wrapper) {
        return wrapper instanceof ObjectWrapper;
    }
    @SuppressWarnings("unchecked")
    private C getWrappedObject(C wrapper) {
        return ((ObjectWrapper<C>) wrapper).getDelegate();
    }

    @Override
    public O getAndSet(C obj, O newValue) {
        if (isWrappedObject(obj)) {
            defaultFieldUpdater.getAndSet(obj, newValue);
            return defaultFieldUpdater.getAndSet(getWrappedObject(obj), newValue);
        } else {
            return defaultFieldUpdater.getAndSet(obj, newValue);
        }
    }

    @Override
    public boolean compareAndSet(C obj, O expect, O update) {
        if (isWrappedObject(obj)) {
            defaultFieldUpdater.compareAndSet(obj, expect, update);
            return defaultFieldUpdater.compareAndSet(getWrappedObject(obj), expect, update);
        } else {
            return defaultFieldUpdater.compareAndSet(obj, expect, update);
        }
    }

    @Override
    public boolean weakCompareAndSet(C obj, O expect, O update) {
        if (isWrappedObject(obj)) {
            defaultFieldUpdater.weakCompareAndSet(obj, expect, update);
            return defaultFieldUpdater.weakCompareAndSet(getWrappedObject(obj), expect, update);
        } else {
            return defaultFieldUpdater.weakCompareAndSet(obj, expect, update);
        }
    }

    @Override
    public void set(C obj, O newValue) {
        defaultFieldUpdater.set(obj, newValue);
        if (isWrappedObject(obj)) defaultFieldUpdater.set(getWrappedObject(obj), newValue);
    }

    @Override
    public void lazySet(C obj, O newValue) {
        defaultFieldUpdater.lazySet(obj, newValue);
        if (isWrappedObject(obj)) defaultFieldUpdater.lazySet(getWrappedObject(obj), newValue);
    }

    @Override
    public O get(C obj) {
        if (isWrappedObject(obj)) {
            defaultFieldUpdater.get(obj);
            return defaultFieldUpdater.get(getWrappedObject(obj));
        } else {
            return defaultFieldUpdater.get(obj);
        }
    }
}
