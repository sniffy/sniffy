package com.github.bedrin.jdbc.sniffer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;

public class ConnectionInvocationHandler implements InvocationHandler {

    private final Connection delegate;

    public ConnectionInvocationHandler(Connection delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(delegate, args);
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
                    new StatementInvocationHandler(result)
            );
        }  else if ("prepareCall".equals(method.getName())) {
            return Proxy.newProxyInstance(
                    ConnectionInvocationHandler.class.getClassLoader(),
                    new Class[]{CallableStatement.class},
                    new StatementInvocationHandler(result)
            );
        } else {
            return result;
        }
    }


}
