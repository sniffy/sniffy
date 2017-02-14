package io.sniffy.sql;

import io.sniffy.Sniffy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

class ConnectionInvocationHandler extends SniffyInvocationHandler<Connection> {

    ConnectionInvocationHandler(Connection delegate, String url, String userName) {
        super(delegate, url, userName);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        checkConnectionAllowed();

        long start = System.currentTimeMillis();
        try {
            Sniffy.enterJdbcMethod();
            Object result = invokeTarget(method, args);
            if ("createStatement".equals(method.getName())) {
                return Proxy.newProxyInstance(
                        ConnectionInvocationHandler.class.getClassLoader(),
                        new Class[]{Statement.class},
                        new StatementInvocationHandler(result, proxy, url, userName)
                );
            } else if ("prepareStatement".equals(method.getName())) {
                return Proxy.newProxyInstance(
                        ConnectionInvocationHandler.class.getClassLoader(),
                        new Class[]{PreparedStatement.class},
                        new PreparedStatementInvocationHandler(result, proxy, url, userName, String.class.cast(args[0]))
                );
            } else if ("prepareCall".equals(method.getName())) {
                return Proxy.newProxyInstance(
                        ConnectionInvocationHandler.class.getClassLoader(),
                        new Class[]{CallableStatement.class},
                        new PreparedStatementInvocationHandler(result, proxy, url, userName, String.class.cast(args[0]))
                );
            } else if ("equals".equals(method.getName())) {
                Object that = args[0];
                return null == that ? Boolean.FALSE :
                        Proxy.isProxyClass(that.getClass()) ? proxy == args[0] : result;
            } else {
                // TODO: proxy other classes which can produce network like getDatabaseMetaData() and others
                return result;
            }
        } finally {
            Sniffy.exitJdbcMethod(method, System.currentTimeMillis() - start);
        }
    }


}
