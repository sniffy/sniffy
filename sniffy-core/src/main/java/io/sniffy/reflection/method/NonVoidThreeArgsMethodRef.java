package io.sniffy.reflection.method;

import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonVoidThreeArgsMethodRef<T, C, P1, P2, P3> extends AbstractMethodRef<C> {

    public NonVoidThreeArgsMethodRef(Method method, Throwable throwable) {
        super(method, throwable);
    }

    public T invoke(C instance, P1 p1, P2 p2, P3 p3) throws UnsafeException {
        try {
            if (null != throwable) {
                throw ExceptionUtil.throwException(throwable);
            } else {
                Object result = method.invoke(instance, p1, p2, p3);
                //noinspection unchecked
                return (T) result;
            }
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        }
    }

}
