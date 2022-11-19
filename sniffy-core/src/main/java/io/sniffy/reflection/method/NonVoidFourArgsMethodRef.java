package io.sniffy.reflection.method;

import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonVoidFourArgsMethodRef<T, C, P1, P2, P3, P4> extends AbstractMethodRef<C> {

    public NonVoidFourArgsMethodRef(Method method, Throwable throwable) {
        super(method, throwable);
    }

    public T invoke(C instance, P1 p1, P2 p2, P3 p3, P4 p4) throws UnsafeException {
        try {
            Object result = method.invoke(instance, p1, p2, p3, p4);
            //noinspection unchecked
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        }
    }

}
