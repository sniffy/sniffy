package io.sniffy.sql;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

class ConnectionInvocationHandler extends SniffyInvocationHandler<Connection> {

    public static final String CREATE_STATEMENT = "createStatement";
    public static final String PREPARE_STATEMENT = "prepareStatement";
    public static final String PREPARE_CALL = "prepareCall";

    ConnectionInvocationHandler(Connection delegate, String url, String userName) {
        super(null, delegate, url, userName);
    }

    public Object invokeImpl(Connection proxy, String methodName, Method method, Object[] args) throws Throwable {

        checkConnectionAllowed(); // TODO: move to invokeTarget() ???

        Object result = invokeTarget(method, args);

        if (CREATE_STATEMENT.equals(methodName)) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{Statement.class},
                    new StatementInvocationHandler<Statement>((Statement) result, connectionProxy, url, userName)
            );
        } else if (PREPARE_STATEMENT.equals(methodName)) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{PreparedStatement.class},
                    new PreparedStatementInvocationHandler<PreparedStatement>((PreparedStatement) result, connectionProxy, url, userName, String.class.cast(args[0]))
            );
        } else if (PREPARE_CALL.equals(methodName)) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{CallableStatement.class},
                    new PreparedStatementInvocationHandler<CallableStatement>((CallableStatement) result, connectionProxy, url, userName, String.class.cast(args[0]))
            );
        } else if ("equals".equals(methodName)) {
            Object that = args[0];
            return null == that ? Boolean.FALSE :
                    Proxy.isProxyClass(that.getClass()) ? proxy == args[0] : result;
        } else {
            // TODO: proxy other classes which can produce network like getDatabaseMetaData() and others
            return result;
        }

    }


}
