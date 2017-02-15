package io.sniffy.sql;

import io.sniffy.Constants;
import io.sniffy.Sniffy;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import static io.sniffy.registry.ConnectionsRegistry.ConnectionStatus.CLOSED;

/**
 * Enable JDBC Sniffer by adding a {@code sniffy:} prefix to your JDBC URL.
 * For example:
 * {@code sniffy:jdbc:h2:mem:}
 *
 * After that you'll be able to verify the number of executed statements using the {@link Sniffy} class
 * @see Sniffy
 * @since 3.1
 */
public class SniffyDriver implements Driver, Constants {

    private static final SniffyDriver INSTANCE = new SniffyDriver();

    private final static Method CONNECT_METHOD;
    private final static Method CONNECT_METHOD_IMPL;

    static {
        Method getConnectionMethod;
        try {
            getConnectionMethod = Driver.class.getMethod("connect", String.class, Properties.class);
        } catch (NoSuchMethodException e) {
            getConnectionMethod = null;
        }

        CONNECT_METHOD = getConnectionMethod;

        Method getConnectionMethodImpl;
        try {
            getConnectionMethodImpl = SniffyDriver.class.getMethod("connect", String.class, Properties.class);
        } catch (NoSuchMethodException e) {
            getConnectionMethodImpl = null;
        }

        CONNECT_METHOD_IMPL = getConnectionMethodImpl;
    }

    static {
        load();
    }

    private static void load() {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Sniffy.initialize();

    }

    protected static void checkConnectionAllowed(Connection connection, String url, String userName) throws SQLException {
        if (CLOSED == ConnectionsRegistry.INSTANCE.resolveDataSourceStatus(url, userName)) {
            try {
                connection.close();
            } catch (Exception e) {
                //
            }
            throw new SQLException(String.format("Connection to %s (%s) refused by Sniffy", url, userName));
        }
    }

    protected static void checkConnectionAllowed(String url, String userName) throws SQLException {
        if (CLOSED == ConnectionsRegistry.INSTANCE.resolveDataSourceStatus(url, userName)) {
            throw new SQLException(String.format("Connection to %s (%s) refused by Sniffy", url, userName));
        }
    }

    public Connection connect(String url, Properties info) throws SQLException {

        if (null == url || !acceptsURL(url)) return null;

        String originUrl = extractOriginUrl(url);
        String userName = info.getProperty("user");

        checkConnectionAllowed(originUrl, userName);

        Driver originDriver;
        try {
            originDriver = DriverManager.getDriver(originUrl);
        } catch (SQLException e) {
            try {
                reloadServiceProviders();
                originDriver = DriverManager.getDriver(originUrl);
            } catch (Exception e2) {
                throw e;
            }
        }

        if (!SniffyConfiguration.INSTANCE.isMonitorJdbc()) return originDriver.connect(originUrl, info);

        long start = System.currentTimeMillis();
        try {
            Sniffy.enterJdbcMethod();
            Connection delegateConnection = originDriver.connect(originUrl, info);
            return Connection.class.cast(Proxy.newProxyInstance(
                    SniffyDriver.class.getClassLoader(),
                    new Class[]{Connection.class},
                    new ConnectionInvocationHandler(delegateConnection, originUrl, userName)
            ));
        } finally {
            Sniffy.exitJdbcMethod(CONNECT_METHOD, System.currentTimeMillis() - start, CONNECT_METHOD_IMPL);
        }
    }

    private void reloadServiceProviders() {

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {

                ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);
                Iterator<Driver> driversIterator = loadedDrivers.iterator();

                try {
                    while (driversIterator.hasNext()) {
                        driversIterator.next();
                    }
                } catch (Throwable t) {
                    // Do nothing
                }
                return null;
            }
        });

    }

    private Driver getOriginDriver(String url) throws SQLException {
        String originUrl = extractOriginUrl(url);
        return DriverManager.getDriver(originUrl);
    }

    protected String extractOriginUrl(String url) {
        if (null == url) return null;
        if (url.startsWith(DRIVER_PREFIX)) return url.substring(DRIVER_PREFIX.length());
        return url;
    }

    public boolean acceptsURL(String url) throws SQLException {
        return null != url && url.startsWith(DRIVER_PREFIX);
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        Driver originDriver = getOriginDriver(url);
        return originDriver.getPropertyInfo(url, info);
    }

    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    public boolean jdbcCompliant() {
        return true;
    }

    public Logger getParentLogger() {
        String message = "getParentLogger() method is not implemented in Sniffy";
        if (!ExceptionUtil.throwException("java.sql.SQLFeatureNotSupportedException", message)) {
            throw new UnsupportedOperationException(message);
        }
        return null;
    }

}
