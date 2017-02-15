package io.sniffy.sql;

import io.sniffy.Sniffy;
import io.sniffy.configuration.SniffyConfiguration;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

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

    private final static Method GET_CONNECTION_METHOD =
            getMethod(DataSource.class, "getConnection");
    private final static Method GET_CONNECTION_WITH_CREDENTIALS_METHOD =
            getMethod(DataSource.class, "getConnection", String.class, String.class);

    private final static Method GET_CONNECTION_METHOD_IMPL =
            getMethod(SniffyDataSource.class, "getConnection");
    private final static Method GET_CONNECTION_WITH_CREDENTIALS_METHOD_IMPL =
            getMethod(SniffyDataSource.class, "getConnection", String.class, String.class);

    private final static Method GET_XA_CONNECTION_METHOD =
            getMethod(XADataSource.class, "getXAConnection");
    private final static Method GET_XA_CONNECTION_WITH_CREDENTIALS_METHOD =
            getMethod(XADataSource.class, "getXAConnection", String.class, String.class);

    private final static Method GET_XA_CONNECTION_METHOD_IMPL =
            getMethod(SniffyDataSource.class, "getXAConnection");
    private final static Method GET_XA_CONNECTION_WITH_CREDENTIALS_METHOD_IMPL =
            getMethod(SniffyDataSource.class, "getXAConnection", String.class, String.class);

    private final static Method GET_POOLED_CONNECTION_METHOD =
            getMethod(ConnectionPoolDataSource.class, "getPooledConnection");
    private final static Method GET_POOLED_CONNECTION_WITH_CREDENTIALS_METHOD =
            getMethod(ConnectionPoolDataSource.class, "getPooledConnection", String.class, String.class);

    private final static Method GET_POOLED_CONNECTION_METHOD_IMPL =
            getMethod(SniffyDataSource.class, "getPooledConnection");
    private final static Method GET_POOLED_CONNECTION_WITH_CREDENTIALS_METHOD_IMPL =
            getMethod(SniffyDataSource.class, "getPooledConnection", String.class, String.class);

    private static Method getMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            // TODO: log me may be?
            return null;
        }
    }

    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... arguments) {
        try {
            return clazz.getMethod(methodName, arguments);
        } catch (NoSuchMethodException e) {
            // TODO: log me may be?
            return null;
        }
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

        if (!SniffyConfiguration.INSTANCE.isMonitorJdbc()) return dataSource.getConnection();

        Connection delegateConnection;
        long start = System.currentTimeMillis();

        String url;
        String userName;

        try {
            Sniffy.enterJdbcMethod();
            delegateConnection = dataSource.getConnection();

            // TODO: cache these parameters
            url = delegateConnection.getMetaData().getURL();
            userName = delegateConnection.getMetaData().getUserName();

            SniffyDriver.checkConnectionAllowed(url, userName);
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_METHOD, System.currentTimeMillis() - start, GET_CONNECTION_METHOD_IMPL);
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

        if (!SniffyConfiguration.INSTANCE.isMonitorJdbc()) return dataSource.getConnection(userName, password);

        Connection delegateConnection;
        long start = System.currentTimeMillis();

        String url;

        try {
            Sniffy.enterJdbcMethod();
            delegateConnection = dataSource.getConnection(userName, password);

            url = delegateConnection.getMetaData().getURL();

            SniffyDriver.checkConnectionAllowed(url, userName);
        } finally {
            Sniffy.exitJdbcMethod(GET_CONNECTION_WITH_CREDENTIALS_METHOD, System.currentTimeMillis() - start, GET_CONNECTION_WITH_CREDENTIALS_METHOD_IMPL);
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

        if (!SniffyConfiguration.INSTANCE.isMonitorJdbc()) return xaDataSource.getXAConnection();

        long start = System.currentTimeMillis();

        try {
            Sniffy.enterJdbcMethod();
            return XAConnection.class.cast(Proxy.newProxyInstance(
                    SniffyDriver.class.getClassLoader(),
                    new Class[]{XAConnection.class},
                    new PooledConnectionInvocationHandler(xaDataSource.getXAConnection())
            ));
        } finally {
            Sniffy.exitJdbcMethod(GET_XA_CONNECTION_METHOD, System.currentTimeMillis() - start, GET_XA_CONNECTION_METHOD_IMPL);
        }

    }

    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {

        if (null == dataSource) throw new SQLException("Target is not a XADataSource instance");

        if (!SniffyConfiguration.INSTANCE.isMonitorJdbc()) return xaDataSource.getXAConnection(user, password);

        long start = System.currentTimeMillis();

        try {
            Sniffy.enterJdbcMethod();
            return XAConnection.class.cast(Proxy.newProxyInstance(
                    SniffyDriver.class.getClassLoader(),
                    new Class[]{XAConnection.class},
                    new PooledConnectionInvocationHandler(xaDataSource.getXAConnection(user, password))
            ));
        } finally {
            Sniffy.exitJdbcMethod(GET_XA_CONNECTION_WITH_CREDENTIALS_METHOD, System.currentTimeMillis() - start, GET_XA_CONNECTION_WITH_CREDENTIALS_METHOD_IMPL);
        }

    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {

        if (null == connectionPoolDataSource) throw new SQLException("Target is not a ConnectionPoolDataSource instance");

        if (!SniffyConfiguration.INSTANCE.isMonitorJdbc()) return connectionPoolDataSource.getPooledConnection();

        long start = System.currentTimeMillis();

        try {
            Sniffy.enterJdbcMethod();
            return PooledConnection.class.cast(Proxy.newProxyInstance(
                    SniffyDriver.class.getClassLoader(),
                    new Class[]{PooledConnection.class},
                    new PooledConnectionInvocationHandler(connectionPoolDataSource.getPooledConnection())
            ));
        } finally {
            Sniffy.exitJdbcMethod(GET_POOLED_CONNECTION_METHOD, System.currentTimeMillis() - start, GET_POOLED_CONNECTION_METHOD_IMPL);
        }

    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {

        if (null == connectionPoolDataSource) throw new SQLException("Target is not a ConnectionPoolDataSource instance");

        if (!SniffyConfiguration.INSTANCE.isMonitorJdbc()) return connectionPoolDataSource.getPooledConnection(user, password);

        long start = System.currentTimeMillis();
        try {
            Sniffy.enterJdbcMethod();
            return PooledConnection.class.cast(Proxy.newProxyInstance(
                    SniffyDriver.class.getClassLoader(),
                    new Class[]{PooledConnection.class},
                    new PooledConnectionInvocationHandler(connectionPoolDataSource.getPooledConnection(user, password))
            ));
        } finally {
            Sniffy.exitJdbcMethod(GET_POOLED_CONNECTION_WITH_CREDENTIALS_METHOD, System.currentTimeMillis() - start, GET_POOLED_CONNECTION_WITH_CREDENTIALS_METHOD_IMPL);
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
    @IgnoreJRERequirement
    @SuppressWarnings("Since15")
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
