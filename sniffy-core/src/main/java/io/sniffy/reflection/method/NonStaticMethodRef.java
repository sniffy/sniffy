package io.sniffy.reflection.method;

import io.sniffy.reflection.UnsafeInvocationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonStaticMethodRef<C> {

    private final Method method;

    public NonStaticMethodRef(Method method) {
        this.method = method;
    }

    public <T> T invoke(C instance, Object... parameters) throws UnsafeInvocationException, InvocationTargetException {
        try {
            //noinspection unchecked
            return (T) method.invoke(instance, parameters);
        } catch (InvocationTargetException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsafeInvocationException(e);
        }
    }

}
