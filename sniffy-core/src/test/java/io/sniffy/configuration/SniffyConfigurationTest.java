package io.sniffy.configuration;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

        // overriden value - can be enabled but cannot be disabled
        System.getProperties().remove("io.sniffy.monitorSocket");
        sniffyConfiguration.loadSniffyConfiguration();
        sniffyConfiguration.setMonitorSocket(false);
        assertTrue(sniffyConfiguration.isMonitorSocket());

        System.setProperty("io.sniffy.monitorSocket", "false");
        sniffyConfiguration.loadSniffyConfiguration();
        assertFalse(sniffyConfiguration.isMonitorSocket());
        sniffyConfiguration.setMonitorSocket(true);
        assertTrue(sniffyConfiguration.isMonitorSocket());

    }

    @Test
    public void testFilterEnabled() {

        SniffyConfiguration sniffyConfiguration = SniffyConfiguration.INSTANCE;

        // enabled
        System.setProperty("io.sniffy.filterEnabled", "true");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.getFilterEnabled());

        System.setProperty("io.sniffy.filterEnabled", "TRUE");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.getFilterEnabled());

        // disabled
        System.setProperty("io.sniffy.filterEnabled", "false");
        sniffyConfiguration.loadSniffyConfiguration();
        assertFalse(sniffyConfiguration.getFilterEnabled());

        System.setProperty("io.sniffy.filterEnabled", "");
        sniffyConfiguration.loadSniffyConfiguration();
        assertFalse(sniffyConfiguration.getFilterEnabled());

        // default value
        System.getProperties().remove("io.sniffy.filterEnabled");
        sniffyConfiguration.loadSniffyConfiguration();
        assertNull(sniffyConfiguration.getFilterEnabled());

        // overriden value
        System.getProperties().remove("io.sniffy.filterEnabled");
        sniffyConfiguration.loadSniffyConfiguration();
        sniffyConfiguration.setFilterEnabled(false);
        assertFalse(sniffyConfiguration.getFilterEnabled());

    }

    @Test
    public void testInjectHtml() {

        SniffyConfiguration sniffyConfiguration = SniffyConfiguration.INSTANCE;

        // enabled
        System.setProperty("io.sniffy.injectHtml", "true");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.getInjectHtmlEnabled());

        System.setProperty("io.sniffy.injectHtml", "TRUE");
        sniffyConfiguration.loadSniffyConfiguration();
        assertTrue(sniffyConfiguration.getInjectHtmlEnabled());

        // disabled
        System.setProperty("io.sniffy.injectHtml", "false");
        sniffyConfiguration.loadSniffyConfiguration();
        assertFalse(sniffyConfiguration.getInjectHtmlEnabled());

        System.setProperty("io.sniffy.injectHtml", "");
        sniffyConfiguration.loadSniffyConfiguration();
        assertFalse(sniffyConfiguration.getInjectHtmlEnabled());

        // default value
        System.getProperties().remove("io.sniffy.injectHtml");
        sniffyConfiguration.loadSniffyConfiguration();
        assertNull(sniffyConfiguration.getInjectHtmlEnabled());

        // overriden value
        System.getProperties().remove("io.sniffy.injectHtml");
        sniffyConfiguration.loadSniffyConfiguration();
        sniffyConfiguration.setInjectHtmlEnabled(false);
        assertFalse(sniffyConfiguration.getInjectHtmlEnabled());

    }

}
