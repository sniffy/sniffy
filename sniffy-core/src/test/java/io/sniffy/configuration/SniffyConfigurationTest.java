package io.sniffy.configuration;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SniffyConfigurationTest {

    private static Properties systemProperties;

    @BeforeClass
    public static void backupSystemPropertiesAndEnvironmentVariables() {
        systemProperties = new Properties(System.getProperties());
    }

    @After
    public void restoreSystemPropertiesAndEnvironmentVariables() {
        System.setProperties(systemProperties);
    }

    @Test
    public void testMonitorJdbc() {

        SniffyConfiguration sniffyConfiguration = SniffyConfiguration.INSTANCE;

        // enabled
        System.setProperty("io.sniffy.monitorJdbc", "true");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.isMonitorJdbc());

        System.setProperty("io.sniffy.monitorJdbc", "TRUE");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.isMonitorJdbc());

        // disabled
        System.setProperty("io.sniffy.monitorJdbc", "false");
        sniffyConfiguration.loadSniffyConfiguration();
        assertFalse(sniffyConfiguration.isMonitorJdbc());

        System.setProperty("io.sniffy.monitorJdbc", "");
        sniffyConfiguration.loadSniffyConfiguration();
        assertFalse(sniffyConfiguration.isMonitorJdbc());

        // default value
        System.getProperties().remove("io.sniffy.monitorJdbc");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.isMonitorJdbc());

        // overriden value
        System.getProperties().remove("io.sniffy.monitorJdbc");
        sniffyConfiguration.loadSniffyConfiguration();
        sniffyConfiguration.setMonitorJdbc(false);
        assertFalse(sniffyConfiguration.isMonitorJdbc());

    }

    @Test
    public void testMonitorSocket() {

        SniffyConfiguration sniffyConfiguration = SniffyConfiguration.INSTANCE;

        // enabled
        System.setProperty("io.sniffy.monitorSocket", "true");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.isMonitorSocket());

        System.setProperty("io.sniffy.monitorSocket", "TRUE");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.isMonitorSocket());

        // disabled
        System.setProperty("io.sniffy.monitorSocket", "false");
        sniffyConfiguration.loadSniffyConfiguration();
        assertFalse(sniffyConfiguration.isMonitorSocket());

        System.setProperty("io.sniffy.monitorSocket", "");
        sniffyConfiguration.loadSniffyConfiguration();
        assertFalse(sniffyConfiguration.isMonitorSocket());

        // default value
        System.getProperties().remove("io.sniffy.monitorSocket");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.isMonitorSocket());

        // overriden value
        System.getProperties().remove("io.sniffy.monitorSocket");
        sniffyConfiguration.loadSniffyConfiguration();
        sniffyConfiguration.setMonitorSocket(false);
        assertFalse(sniffyConfiguration.isMonitorSocket());

    }

}
