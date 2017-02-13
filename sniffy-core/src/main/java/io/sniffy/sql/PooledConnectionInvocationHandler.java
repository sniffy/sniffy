package io.sniffy.sql;

import io.sniffy.Sniffy;

import javax.sql.PooledConnection;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

class PooledConnectionInvocationHandler implements InvocationHandler {

    private final PooledConnection delegate;

    private final static Method GET_CONNECTION_METHOD;

    static {
        Method getConnectionMethod;
        try {
            getConnectionMethod = PooledConnection.class.getMethod("getConnection");
        } catch (NoSuchMethodException e) {
            getConnectionMethod = null;
        }

        GET_CONNECTION_METHOD = getConnectionMethod;
    }

    PooledConnectionInvocationHandler(PooledConnection delegate) {
        this.delegate = delegate;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if ("getConnection".equals(method.getName())) {
            long start = System.currentTimeMillis();
            try {
                Sniffy.enterJdbcMethod();

                Connection targetConnection = Connection.class.cast(method.invoke(delegate, args));

                String url = targetConnection.getMetaData().getURL();
                String userName = targetConnection.getMetaData().getUserName();

                SniffyDriver.checkConnectionAllowed(targetConnection, url, userName);

                return Connection.class.cast(Proxy.newProxyInstance(
                        PooledConnectionInvocationHandler.class.getClassLoader(),
                        new Class[]{Connection.class},
                        new ConnectionInvocationHandler(targetConnection, url, userName)
                ));
            } finally {
                Sniffy.exitJdbcMethod(GET_CONNECTION_METHOD, System.currentTimeMillis() - start);
            }
        } else {
            return method.invoke(delegate, args);
        }
    }


}
