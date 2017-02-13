package io.sniffy.sql;

import io.sniffy.Sniffy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

class SniffyInvocationHandler<T> implements InvocationHandler {

    protected final T delegate;

    protected final String url;
    protected final String userName;

    SniffyInvocationHandler(T delegate, String url, String userName) {
        this.delegate = delegate;
        this.url = url;
        this.userName = userName;
    }

    protected void checkConnectionAllowed() throws SQLException {
        SniffyDriver.checkConnectionAllowed(url, userName);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return invokeTarget(method, args);
    }

    protected Object invokeTarget(Method method, Object[] args) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Sniffy.enterJdbcMethod();
            return invokeTargetImpl(method, args);
        } finally {
            Sniffy.exitJdbcMethod(method, System.currentTimeMillis() - start);
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
