package io.sniffy.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;

public class ConnectionInvocationHandler extends SniffyInvocationHandler<Connection> implements InvocationHandler {

    public ConnectionInvocationHandler(Connection delegate) {
        super(delegate);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = invokeTarget(method, args);
        if ("createStatement".equals(method.getName())) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{Statement.class},
                    new StatementInvocationHandler(result)
            );
        } else if ("prepareStatement".equals(method.getName())) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{PreparedStatement.class},
                    new PreparedStatementInvocationHandler(result, String.class.cast(args[0]))
            );
        }  else if ("prepareCall".equals(method.getName())) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{CallableStatement.class},
                    new PreparedStatementInvocationHandler(result, String.class.cast(args[0]))
            );
        } else {
            return result;
        }
    }


}
