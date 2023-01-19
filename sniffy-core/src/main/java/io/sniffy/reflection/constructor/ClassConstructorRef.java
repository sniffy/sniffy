package io.sniffy.reflection.constructor;

import io.sniffy.reflection.Unsafe;
import io.sniffy.reflection.UnsafeInvocationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ClassConstructorRef<C> {

    private final Constructor<C> constructor;

    public ClassConstructorRef(Constructor<C> constructor) {
        this.constructor = constructor;
        // TODO: should we make it accessible here ?
    }

    public C newInstanceOrNull(Object... parameters) {
        try {
            return newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    public C newInstance(Object... parameters) throws UnsafeInvocationException {
        try {
            return constructor.newInstance(parameters);
        } catch (InvocationTargetException e) {
            throw Unsafe.throwException(e.getTargetException());
        } catch (Throwable e) {
            throw new UnsafeInvocationException(e);
        }
    }

}
