package io.sniffy.reflection.method;

import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonVoidTwoArgsMethodRef<T, C, P1, P2> extends AbstractMethodRef<C> {

    public NonVoidTwoArgsMethodRef(Method method, Throwable throwable) {
        super(method, throwable);
    }

    public T invoke(C instance, P1 p1, P2 p2) throws UnsafeException {
        try {
            Object result = method.invoke(instance, p1, p2);
            //noinspection unchecked
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        }
    }

}