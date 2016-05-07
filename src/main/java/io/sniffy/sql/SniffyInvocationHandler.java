package io.sniffy.sql;

import io.sniffy.Sniffer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by bedrin on 08.05.2016.
 */
public class SniffyInvocationHandler<T> {

    protected final T delegate;

    public SniffyInvocationHandler(T delegate) {
        this.delegate = delegate;
    }

    protected Object invokeTarget(Method method, Object[] args) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Sniffer.enterJdbcMethod();
            return invokeTargetImpl(method, args);
        } finally {
            Sniffer.exitJdbcMethod(method, System.currentTimeMillis() - start);
        }
    }

    protected Object invokeTargetImpl(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
