package io.sniffy.sql;

import io.sniffy.Sniffy;

import javax.sql.*;
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
public class SniffyDataSource implements DataSource, XADataSource, ConnectionPoolDataSource {

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

    private final CommonDataSource target;
    private final DataSource dataSource;
    private final XADataSource xaDataSource;
    private final ConnectionPoolDataSource connectionPoolDataSource;

    @SuppressWarnings("unchecked")
    public static <
            T extends CommonDataSource,
            R extends DataSource & XADataSource & ConnectionPoolDataSource> R wrap(T target) {
        return (R) new SniffyDataSource(target);
    }

    public <T extends CommonDataSource> SniffyDataSource(T target) {
        this.target = target;
        this.dataSource = target instanceof DataSource ? DataSource.class.cast(target) : null;
        this.xaDataSource = target instanceof XADataSource ? XADataSource.class.cast(target) : null;
        this.connectionPoolDataSource = target instanceof ConnectionPoolDataSource ? ConnectionPoolDataSource.class.cast(target) : null;
    }

    @Override
    public Connection getConnection() throws SQLException {

        if (null == dataSource) throw new SQLException("Target is not a DataSource instance");

        Connection delegateConnection;
        long start = System.currentTimeMillis();

        String url;
        String userName;

        try {
            Sniffy.enterJdbcMethod();
            delegateConnection = dataSource.getConnection();

            url = delegateConnection.getMetaData().getURL();
            userName = delegateConnection.getMetaData().getUserName();

            SniffyDriver.checkConnectionAllowed(url, userName);
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_METHOD, System.currentTimeMillis() - start);
        }

        return Connection.class.cast(Proxy.newProxyInstance(
                SniffyDriver.class.getClassLoader(),
                new Class[]{Connection.class},
                new ConnectionInvocationHandler(delegateConnection, url, userName)
        ));
    }

    @Override
    public Connection getConnection(String userName, String password) throws SQLException {

        if (null == dataSource) throw new SQLException("Target is not a DataSource instance");

        Connection delegateConnection;
        long start = System.currentTimeMillis();

        String url;

        try {
            Sniffy.enterJdbcMethod();
            delegateConnection = dataSource.getConnection(userName, password);

            url = delegateConnection.getMetaData().getURL();

            SniffyDriver.checkConnectionAllowed(url, userName);
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_WITH_CREDENTIALS_METHOD, System.currentTimeMillis() - start);
        }

        return Connection.class.cast(Proxy.newProxyInstance(
                SniffyDriver.class.getClassLoader(),
                new Class[]{Connection.class},
                new ConnectionInvocationHandler(delegateConnection, url, userName)
        ));
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {

        if (null == dataSource) throw new SQLException("Target is not a XADataSource instance");

        long start = System.currentTimeMillis();

        try {
            Sniffy.enterJdbcMethod();
            return XAConnection.class.cast(Proxy.newProxyInstance(
                    SniffyDriver.class.getClassLoader(),
                    new Class[]{XAConnection.class},
                    new PooledConnectionInvocationHandler(xaDataSource.getXAConnection())
            ));
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_METHOD, System.currentTimeMillis() - start);
        }

    }

    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {

        if (null == dataSource) throw new SQLException("Target is not a XADataSource instance");

        long start = System.currentTimeMillis();

        try {
            Sniffy.enterJdbcMethod();
            return XAConnection.class.cast(Proxy.newProxyInstance(
                    SniffyDriver.class.getClassLoader(),
                    new Class[]{XAConnection.class},
                    new PooledConnectionInvocationHandler(xaDataSource.getXAConnection(user, password))
            ));
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_METHOD, System.currentTimeMillis() - start);
        }

    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {

        if (null == connectionPoolDataSource) throw new SQLException("Target is not a ConnectionPoolDataSource instance");

        long start = System.currentTimeMillis();

        try {
            Sniffy.enterJdbcMethod();
            return PooledConnection.class.cast(Proxy.newProxyInstance(
                    SniffyDriver.class.getClassLoader(),
                    new Class[]{PooledConnection.class},
                    new PooledConnectionInvocationHandler(connectionPoolDataSource.getPooledConnection())
            ));
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_METHOD, System.currentTimeMillis() - start);
        }

    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {

        if (null == connectionPoolDataSource) throw new SQLException("Target is not a ConnectionPoolDataSource instance");

        long start = System.currentTimeMillis();
        try {
            Sniffy.enterJdbcMethod();
            return PooledConnection.class.cast(Proxy.newProxyInstance(
                    SniffyDriver.class.getClassLoader(),
                    new Class[]{PooledConnection.class},
                    new PooledConnectionInvocationHandler(connectionPoolDataSource.getPooledConnection(user, password))
            ));
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_METHOD, System.currentTimeMillis() - start);
        }

    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(target.getClass())) {
            return iface.cast(target);
        } else {
            return dataSource.unwrap(iface);
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(target.getClass()) || dataSource.isWrapperFor(iface);
    }

}
