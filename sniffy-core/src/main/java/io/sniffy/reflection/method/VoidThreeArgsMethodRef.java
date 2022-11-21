package io.sniffy.reflection.method;

import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VoidThreeArgsMethodRef<C, P1, P2, P3> extends AbstractMethodRef<C> {

    public VoidThreeArgsMethodRef(Method method, Throwable throwable) {
        super(method, throwable);
    }

    public void invoke(C instance, P1 p1, P2 p2, P3 p3) throws UnsafeException {
        try {
            if (null != throwable) {
                throw ExceptionUtil.throwException(throwable);
            } else {
                method.invoke(instance, p1, p2, p3);
            }
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        }
    }

}
