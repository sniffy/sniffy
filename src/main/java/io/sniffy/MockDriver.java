package io.sniffy;

import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Enable JDBC Sniffer by adding a {@code sniffer:} prefix to your JDBC URL.
 * For example:
 * {@code sniffer:jdbc:h2:mem:}
 *
 * After that you'll be able to verify the number of executed statements using the {@link Sniffer} class
 * @see Sniffer
 */
public class MockDriver implements Driver {

    private static final MockDriver INSTANCE = new MockDriver();

    static {
        load();
    }

    private static void load() {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection connect(String url, Properties info) throws SQLException {
        String originUrl = extractOriginUrl(url);
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
        Connection delegateConnection = originDriver.connect(originUrl, info);

        return Connection.class.cast(Proxy.newProxyInstance(
                MockDriver.class.getClassLoader(),
                new Class[]{Connection.class},
                new ConnectionInvocationHandler(delegateConnection)
        ));
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

    private static String extractOriginUrl(String url) {
        if (null == url) return null;
        if (url.startsWith(Constants.DRIVER_PREFIX)) return url.substring(Constants.DRIVER_PREFIX.length());
        return url;
    }

    public boolean acceptsURL(String url) throws SQLException {
        return null != url && url.startsWith(Constants.DRIVER_PREFIX);
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        Driver originDriver = getOriginDriver(url);
        return originDriver.getPropertyInfo(url, info);
    }

    public int getMajorVersion() {
        return Constants.MAJOR_VERSION;
    }

    public int getMinorVersion() {
        return Constants.MINOR_VERSION;
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
