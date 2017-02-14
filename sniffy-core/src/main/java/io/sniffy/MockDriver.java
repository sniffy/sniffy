package io.sniffy;

import io.sniffy.sql.SniffyDriver;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @see SniffyDriver
 */
@Deprecated
public class MockDriver extends SniffyDriver {

    private static final MockDriver INSTANCE = new MockDriver();

    private static final String LEGACY_DRIVER_PREFIX = "sniffer:";

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

    @Override
    protected String extractOriginUrl(String url) {
        if (null == url) return null;
        if (url.startsWith(LEGACY_DRIVER_PREFIX)) return url.substring(LEGACY_DRIVER_PREFIX.length());
        return url;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return null != url && url.startsWith(LEGACY_DRIVER_PREFIX);
    }

}
