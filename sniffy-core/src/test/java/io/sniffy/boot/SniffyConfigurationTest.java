package io.sniffy.boot;

import io.sniffy.servlet.SnifferFilter;
import org.junit.Test;

import static io.sniffy.boot.SniffyConfiguration.DEFAULT_ENABLED;
import static io.sniffy.boot.SniffyConfiguration.DEFAULT_INJECT_HTML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SniffyConfigurationTest {

    @Test
    public void testConfiguration() {

        SniffyConfiguration sniffyConfiguration = new SniffyConfiguration();

        assertEquals(DEFAULT_ENABLED, sniffyConfiguration.isEnabled());
        assertEquals(DEFAULT_INJECT_HTML, sniffyConfiguration.isInjectHtml());

        SnifferFilter filter = sniffyConfiguration.sniffyFilter();

        assertNotNull(filter);
        assertEquals(DEFAULT_ENABLED, filter.isEnabled());
        assertEquals(DEFAULT_INJECT_HTML, filter.isInjectHtml());

    }

    @Test
    public void testConfigurationDisabled() {

        SniffyConfiguration sniffyConfiguration = new SniffyConfiguration();
        sniffyConfiguration.setEnabled(false);

        assertEquals(false, sniffyConfiguration.isEnabled());
        assertEquals(DEFAULT_INJECT_HTML, sniffyConfiguration.isInjectHtml());

        SnifferFilter filter = sniffyConfiguration.sniffyFilter();

        assertNotNull(filter);
        assertEquals(false, filter.isEnabled());
        assertEquals(DEFAULT_INJECT_HTML, filter.isInjectHtml());

    }

    @Test
    public void testConfigurationDoNotInjectHtml() {

        SniffyConfiguration sniffyConfiguration = new SniffyConfiguration();
        sniffyConfiguration.setInjectHtml(false);

        assertEquals(DEFAULT_ENABLED, sniffyConfiguration.isEnabled());
        assertEquals(false, sniffyConfiguration.isInjectHtml());

        SnifferFilter filter = sniffyConfiguration.sniffyFilter();

        assertNotNull(filter);
        assertEquals(DEFAULT_ENABLED, filter.isEnabled());
        assertEquals(false, filter.isInjectHtml());

    }

    @Test
    public void testConfigurationDisabledDoNotInjectHtml() {

        SniffyConfiguration sniffyConfiguration = new SniffyConfiguration();
        sniffyConfiguration.setEnabled(false);
        sniffyConfiguration.setInjectHtml(false);

        assertEquals(false, sniffyConfiguration.isEnabled());
        assertEquals(false, sniffyConfiguration.isInjectHtml());

        SnifferFilter filter = sniffyConfiguration.sniffyFilter();

        assertNotNull(filter);
        assertEquals(false, filter.isEnabled());
        assertEquals(false, filter.isInjectHtml());

    }

}
