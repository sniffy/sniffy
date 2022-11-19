package io.sniffy.reflection.method;

import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonVoidOneArgMethodRef<T, C, P1> extends AbstractMethodRef<C> {

    public NonVoidOneArgMethodRef(Method method, Throwable throwable) {
        super(method, throwable);
    }

    public T invoke(C instance, P1 p1) throws UnsafeException {
        try {
            Object result = method.invoke(instance, p1);
            //noinspection unchecked
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        }
    }

}
