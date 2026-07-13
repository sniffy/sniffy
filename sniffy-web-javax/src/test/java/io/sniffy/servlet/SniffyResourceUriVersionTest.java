package io.sniffy.servlet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SniffyResourceUriVersionTest {

    @Test
    public void testResourceUriPrefixUsesSniffy4Version() {
        assertEquals("sniffy/4.0.0", SniffyFilter.SNIFFY_RESOURCE_URI_PREFIX);
    }

}
