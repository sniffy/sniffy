package io.sniffy.sql;

import javax.sql.PooledConnection;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class PooledConnectionInvocationHandler extends SniffyInvocationHandler<PooledConnection> {

    public PooledConnectionInvocationHandler(PooledConnection delegate) {
        super(delegate);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = invokeTarget(method, args);
        if ("getConnection".equals(method.getName())) {
            return new ConnectionInvocationHandler(Connection.class.cast(result));
        } else {
            // TODO: proxe other classes which can produce network like getDatabaseMetaData() and others
            return result;
        }
    }


}
