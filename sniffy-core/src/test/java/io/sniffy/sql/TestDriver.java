package io.sniffy.sql;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import static org.mockito.Mockito.spy;

public class TestDriver implements Driver {

    private final static TestDriver INSTANCE = spy(new TestDriver());

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

    public static TestDriver getSpy() {
        return INSTANCE;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return DriverManager.getConnection(getTargetUrl(url), info);
    }

    private String getTargetUrl(String url) {
        return url.replaceAll("jdbc:h2spy:", "jdbc:h2:");
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:h2spy:");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return DriverManager.getDriver(getTargetUrl(url)).getPropertyInfo(getTargetUrl(url), info);
    }

    @Override
    public int getMajorVersion() {
        return 0;
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
        return null;
    }

}
