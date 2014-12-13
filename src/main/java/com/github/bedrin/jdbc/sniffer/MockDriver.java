package com.github.bedrin.jdbc.sniffer;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class MockDriver implements Driver {

    private final static String DRIVER_PREFIX = "sniffer:";

    public MockDriver() {
        try {
            DriverManager.registerDriver(this);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        String originUrl = extractOriginUrl(url);
        Driver originDriver = DriverManager.getDriver(originUrl);
        return new MockConnection(originDriver.connect(originUrl, info));
    }

    private Driver getOriginDriver(String url) throws SQLException {
        String originUrl = extractOriginUrl(url);
        return DriverManager.getDriver(originUrl);
    }

    private static String extractOriginUrl(String url) {
        if (null == url) return null;
        if (url.startsWith(DRIVER_PREFIX)) return url.substring(DRIVER_PREFIX.length());
        return url;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return null != url && url.startsWith(DRIVER_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        Driver originDriver = getOriginDriver(url);
        return originDriver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}
