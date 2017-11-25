package io.sniffy.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Wrapper;

public abstract class JdbcInvocationHandler<T extends Wrapper> implements InvocationHandler {

    public static final String GET_CONNECTION = "getConnection";

    protected final Connection connectionProxy;
    protected final T delegate;

    protected JdbcInvocationHandler(Connection connectionProxy, T delegate) {
        this.connectionProxy = connectionProxy;
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();

        if (connectionProxy != null && GET_CONNECTION.equals(methodName) && returnType.isAssignableFrom(Connection.class)) {
            return connectionProxy;
        } else {
            return invokeImpl((T) proxy, methodName, method, args);
        }
    }

    protected Object invokeImpl(T proxy, String methodName, Method method, Object[] args) throws Throwable {
        return invokeTarget(method, args);
    }

    protected Object invokeTarget(Method method, Object[] args) throws Throwable {
        return invokeTargetImpl(method, args);
    }

    protected Object invokeTargetImpl(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

}
