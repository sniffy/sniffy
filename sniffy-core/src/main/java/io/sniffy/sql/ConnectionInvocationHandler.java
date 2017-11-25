package io.sniffy.sql;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

class ConnectionInvocationHandler extends SniffyInvocationHandler<Connection> {

    ConnectionInvocationHandler(Connection delegate, String url, String userName) {
        super(null, delegate, url, userName);
    }

    public Object invokeImpl(Connection proxy, String methodName, Method method, Object[] args) throws Throwable {

        checkConnectionAllowed(); // TODO: move to invokeTarget() ???

        Object result = invokeTarget(method, args);

        if (CREATE_STATEMENT_METHOD.equals(methodName)) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{Statement.class},
                    new StatementInvocationHandler<Statement>((Statement) result, connectionProxy, url, userName)
            );
        } else if (PREPARE_STATEMENT_METHOD.equals(methodName)) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{PreparedStatement.class},
                    new PreparedStatementInvocationHandler<PreparedStatement>((PreparedStatement) result, connectionProxy, url, userName, String.class.cast(args[0]))
            );
        } else if (PREPARE_CALL_METHOD.equals(methodName)) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{CallableStatement.class},
                    new PreparedStatementInvocationHandler<CallableStatement>((CallableStatement) result, connectionProxy, url, userName, String.class.cast(args[0]))
            );
        } else {
            // TODO: proxy other classes which can produce network like getDatabaseMetaData() and others
            return result;
        }

    }


}
