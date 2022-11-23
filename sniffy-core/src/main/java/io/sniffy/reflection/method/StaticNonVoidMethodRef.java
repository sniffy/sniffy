package io.sniffy.reflection.method;

import io.sniffy.reflection.UnsafeInvocationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StaticNonVoidMethodRef<T> {

    private final Method method;

    public StaticNonVoidMethodRef(Method method) {
        this.method = method;
    }

    public T invoke(Object... parameters) throws UnsafeInvocationException, InvocationTargetException {
        try {
            //noinspection unchecked
            return (T) method.invoke(parameters);
        } catch (InvocationTargetException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsafeInvocationException(e);
        }
    }

}
