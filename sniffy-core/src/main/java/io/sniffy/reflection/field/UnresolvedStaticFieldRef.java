package io.sniffy.reflection.field;

import io.sniffy.reflection.UnresolvedRef;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.UnsafeInvocationException;

public class UnresolvedStaticFieldRef<T> extends UnresolvedRef<StaticFieldRef<T>> {

    public UnresolvedStaticFieldRef(StaticFieldRef<T> ref, Throwable throwable) {
        super(ref, throwable);
    }

    public boolean compareAndSet(T oldValue, T newValue) throws UnresolvedRefException, UnsafeInvocationException {
        return resolve().compareAndSet(oldValue, newValue);
    }

    public boolean trySet(T value) {
        try {
            set(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void set(T value) throws UnresolvedRefException, UnsafeInvocationException {
        resolve().set(value);
    }

    public T getOrDefault(T defaultValue) {
        try {
            return get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public T get() throws UnresolvedRefException, UnsafeInvocationException {
        return resolve().get();
    }

}
