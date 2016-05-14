package io.sniffy;

import io.sniffy.sql.SniffyDriver;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Enable JDBC Sniffer by adding a {@code sniffer:} prefix to your JDBC URL.
 * For example:
 * {@code sniffer:jdbc:h2:mem:}
 *
 * After that you'll be able to verify the number of executed statements using the {@link Sniffy} class
 * @see Sniffy
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
