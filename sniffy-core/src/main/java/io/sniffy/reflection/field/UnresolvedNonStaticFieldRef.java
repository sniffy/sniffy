package io.sniffy.reflection.field;

import io.sniffy.reflection.UnresolvedRef;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.UnsafeInvocationException;

public class UnresolvedNonStaticFieldRef<C,T> extends UnresolvedRef<NonStaticFieldRef<C,T>> {

    public UnresolvedNonStaticFieldRef(NonStaticFieldRef<C,T> ref, Throwable throwable) {
        super(ref, throwable);
    }

    public boolean compareAndSet(C instance, T oldValue, T newValue) throws UnresolvedRefException, UnsafeInvocationException {
        return resolve().compareAndSet(instance, oldValue, newValue);
    }

    public boolean trySet(C instance, T value) {
        try {
            set(instance, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void set(C instance, T value) throws UnresolvedRefException, UnsafeInvocationException {
        resolve().set(instance, value);
    }

    public T getOrDefault(C instance, T defaultValue) {
        try {
            return get(instance);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public T get(C instance) throws UnresolvedRefException, UnsafeInvocationException {
        return resolve().get(instance);
    }

    public T getNotNullOrDefault(C instance, T defaultValue) {
        try {
            return getNotNull(instance, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public T getNotNull(C instance, T defaultValue) throws UnresolvedRefException, UnsafeInvocationException {
        return resolve().getNotNull(instance, defaultValue);
    }

    public boolean tryCopy(C from, C to) {
        try {
            resolve().copy(from, to);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public void copy(C from, C to) throws UnsafeInvocationException, UnresolvedRefException {
        resolve().copy(from, to);
    }

}
