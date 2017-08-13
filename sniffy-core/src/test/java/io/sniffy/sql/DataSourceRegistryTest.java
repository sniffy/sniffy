package io.sniffy.sql;

import io.sniffy.registry.ConnectionsRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Map;

import static org.junit.Assert.*;

public class DataSourceRegistryTest {

    @Before
    @After
    public void clearConnectionRules() {
        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void testDataSourceDiscovered() throws SQLException {

        try (Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa")) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
        }

        Map<Map.Entry<String, String>, Integer> discoveredDataSources =
                ConnectionsRegistry.INSTANCE.getDiscoveredDataSources();

        assertNotNull(discoveredDataSources);
        assertEquals(1, discoveredDataSources.size());
        assertEquals(0, discoveredDataSources.get(new AbstractMap.SimpleEntry<String, String>("jdbc:h2:mem:", "sa")).intValue());

    }

    @Test
    public void testDataSourceDisabled() throws SQLException {

        ConnectionsRegistry.INSTANCE.setDataSourceStatus("jdbc:h2:mem:", "sa", -1);

        try {
            DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa");
            fail("Connection should have been forbidden");
        } catch (SQLException e) {
            assertNotNull(e);
        }

    }

}
