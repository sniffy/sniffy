package io.sniffy.sql;

import io.sniffy.Sniffer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SniffyInvocationHandler<T> implements InvocationHandler {

    protected final T delegate;

    public SniffyInvocationHandler(T delegate) {
        this.delegate = delegate;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return invokeTarget(method, args);
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
