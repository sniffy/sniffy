package io.sniffy.reflection;

import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VoidOneArgMethodRef<C, P1> extends AbstractMethodRef<C> {

    public VoidOneArgMethodRef(Method method, Throwable throwable) {
        super(method, throwable);
    }

    public void invoke(C instance, P1 p1) throws UnsafeException {
        try {
            method.invoke(instance, p1);
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        }
    }

}
