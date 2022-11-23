package io.sniffy.reflection.constructor;

import io.sniffy.reflection.UnsafeInvocationException;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ZeroArgsClassConstructorRef<C> {

    private final Constructor<C> constructor;

    public ZeroArgsClassConstructorRef(Constructor<C> constructor) {
        this.constructor = constructor;
        // TODO: should we make it accessible here ?
    }

    public C newInstanceOrNull() {
        try {
            return newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    public C newInstance() throws UnsafeInvocationException {
        try {
            return constructor.newInstance();
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        } catch (Throwable e) {
            throw new UnsafeInvocationException(e);
        }
    }

}
