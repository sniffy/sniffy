package io.sniffy.sql;

import io.sniffy.Sniffy;

import javax.sql.PooledConnection;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

public class PooledConnectionInvocationHandler extends SniffyInvocationHandler<PooledConnection> {

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

    public PooledConnectionInvocationHandler(PooledConnection delegate, String url, String userName) {
        super(delegate, url, userName);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        checkConnectionAllowed();

        if ("getConnection".equals(method.getName())) {
            long start = System.currentTimeMillis();
            try {
                Sniffy.enterJdbcMethod();
                return Connection.class.cast(Proxy.newProxyInstance(
                        PooledConnectionInvocationHandler.class.getClassLoader(),
                        new Class[]{Connection.class},
                        new ConnectionInvocationHandler(Connection.class.cast(invokeTarget(method, args)), url, userName)
                ));
            } finally {
                Sniffy.exitJdbcMethod(GET_CONNECTION_METHOD, System.currentTimeMillis() - start);
            }
        } else {
            return invokeTarget(method, args);
        }
    }


}
