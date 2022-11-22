package io.sniffy.reflection.constructor;

import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.ExceptionUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ZeroArgsConstructorRef<C> extends AbstractConstructorRef<C> {

    public ZeroArgsConstructorRef(Constructor<C> constructor, MethodHandle methodHandle, Throwable throwable) {
        super(constructor, methodHandle, throwable);
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    public C invoke() throws UnsafeException {
        try {
            return (C) constructor.newInstance();
        } catch (InstantiationException e) {
            throw new UnsafeException(e);
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw new UnsafeException(e);
        }
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    public void invoke(C instance) throws UnsafeException {
        try {
            if (null != throwable) {
                throw ExceptionUtil.throwException(throwable);
            } else {
                methodHandle.invoke(instance);
            }
        } catch (WrongMethodTypeException e) {
            throw new UnsafeException(e);
        } catch (ClassCastException e) {
            throw new UnsafeException(e);
        } catch (Throwable e) {
            throw ExceptionUtil.throwException(e);
        }
    }

}
