package io.sniffy.sql;

import io.sniffy.Sniffy;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Wrapper;

class SniffyInvocationHandler<T extends Wrapper> extends JdbcInvocationHandler<T> {

    protected final String url;
    protected final String userName;

    SniffyInvocationHandler(Connection connectionProxy, T delegate, String url, String userName) {
        super(connectionProxy, delegate);
        this.url = url;
        this.userName = userName;
    }

    protected void checkConnectionAllowed() throws SQLException {
        checkConnectionAllowed(false);
    }

    protected void checkConnectionAllowed(boolean sleep) throws SQLException {
        SniffyDriver.checkConnectionAllowed(url, userName, sleep);
    }

    protected Object invokeTarget(Method method, Object[] args) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Sniffy.enterJdbcMethod();
            return invokeTargetImpl(method, args);
        } finally {
            Sniffy.exitJdbcMethod(method, System.currentTimeMillis() - start);
        }
    }

}
