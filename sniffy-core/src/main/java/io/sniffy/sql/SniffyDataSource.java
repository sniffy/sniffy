package io.sniffy.sql;

import io.sniffy.Sniffy;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * @since 3.1
 */
public class SniffyDataSource implements DataSource {

    private final static Method GET_CONNECTION_METHOD;
    private final static Method GET_CONNECTION_WITH_CREDENTIALS_METHOD;

    static {
        Method getConnectionMethod;
        try {
            getConnectionMethod = DataSource.class.getMethod("getConnection");
        } catch (NoSuchMethodException e) {
            getConnectionMethod = null;
        }

        GET_CONNECTION_METHOD = getConnectionMethod;

        Method getConnectionWithCredentialsMethod;
        try {
            getConnectionWithCredentialsMethod = DataSource.class.getMethod("getConnection", String.class, String.class);
        } catch (NoSuchMethodException e) {
            getConnectionWithCredentialsMethod = null;
        }

        GET_CONNECTION_WITH_CREDENTIALS_METHOD = getConnectionWithCredentialsMethod;
    }

    private final DataSource target;

    public SniffyDataSource(DataSource target) {
        this.target = target;
    }

    @Override
    public Connection getConnection() throws SQLException {

        Connection delegateConnection;
        long start = System.currentTimeMillis();

        try {
            Sniffy.enterJdbcMethod();
            delegateConnection = target.getConnection();
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_METHOD, System.currentTimeMillis() - start);
        }

        return Connection.class.cast(Proxy.newProxyInstance(
                SniffyDriver.class.getClassLoader(),
                new Class[]{Connection.class},
                new ConnectionInvocationHandler(delegateConnection)
        ));
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {

        Connection delegateConnection;
        long start = System.currentTimeMillis();

        try {
            Sniffy.enterJdbcMethod();
            delegateConnection = target.getConnection(username, password);
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_WITH_CREDENTIALS_METHOD, System.currentTimeMillis() - start);
        }

        return Connection.class.cast(Proxy.newProxyInstance(
                SniffyDriver.class.getClassLoader(),
                new Class[]{Connection.class},
                new ConnectionInvocationHandler(delegateConnection)
        ));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return target.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        target.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        target.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return target.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return target.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return target.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return target.isWrapperFor(iface);
    }
}
