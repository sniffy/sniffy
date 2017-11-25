package io.sniffy.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;

import static java.lang.Boolean.FALSE;

public class JdbcInvocationHandler<T extends Wrapper> implements InvocationHandler {

    public static final String GET_CONNECTION_METHOD = "getConnection";

    public static final String CREATE_STATEMENT_METHOD = "createStatement";
    public static final String PREPARE_STATEMENT_METHOD = "prepareStatement";
    public static final String PREPARE_CALL_METHOD = "prepareCall";

    public static final String EQUALS_METHOD = "equals";

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

        if (connectionProxy != null && GET_CONNECTION_METHOD.equals(methodName) && returnType.isAssignableFrom(Connection.class)) {
            return connectionProxy;
        } else if (EQUALS_METHOD.equals(methodName)) {
            Object that = args[0];
            if (null == that) {
                return FALSE;
            } else if (Proxy.isProxyClass(that.getClass())) {
                InvocationHandler thatInvocationHandler = Proxy.getInvocationHandler(that);
                if (thatInvocationHandler instanceof JdbcInvocationHandler) {
                    return delegate.equals(((JdbcInvocationHandler) thatInvocationHandler).delegate);
                } else {
                    return FALSE;
                }
            } else {
                return invokeTargetImpl(method, args);
            }
        } else {
            return invokeImpl((T) proxy, methodName, method, args);
        }
    }

    protected Object invokeImpl(T proxy, String methodName, Method method, Object[] args) throws Throwable {
        Object result = invokeTarget(method, args);

        if (CREATE_STATEMENT_METHOD.equals(methodName)) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{Statement.class},
                    new JdbcInvocationHandler<Statement>(connectionProxy, (Statement) result)
            );
        } else if (PREPARE_STATEMENT_METHOD.equals(methodName)) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{PreparedStatement.class},
                    new JdbcInvocationHandler<PreparedStatement>(connectionProxy, (PreparedStatement) result)
            );
        } else if (PREPARE_CALL_METHOD.equals(methodName)) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{CallableStatement.class},
                    new JdbcInvocationHandler<CallableStatement>(connectionProxy, (CallableStatement) result)
            );
        } else {
            // TODO: proxy other classes which might have getConnection method like getDatabaseMetaData() and others
            return result;
        }
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
