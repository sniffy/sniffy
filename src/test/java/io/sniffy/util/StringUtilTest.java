package io.sniffy.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {

    @Test
    public void testEscapeJsonString() throws Exception {
        assertEquals("\"\\\\\\\"\\t\\n\\r\\f\\/\"", StringUtil.escapeJsonString("\\\"\t\n\r\f/"));
        assertEquals("\"\"", StringUtil.escapeJsonString(null));
    }

}