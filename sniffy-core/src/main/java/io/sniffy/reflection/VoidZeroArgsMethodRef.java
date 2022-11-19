package io.sniffy.reflection;

import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VoidZeroArgsMethodRef<C> extends AbstractMethodRef<C> {

    public VoidZeroArgsMethodRef(Method method, Throwable throwable) {
        super(method, throwable);
    }

    public void invoke(C instance) throws UnsafeException {
        try {
            method.invoke(instance);
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        }
    }

}
