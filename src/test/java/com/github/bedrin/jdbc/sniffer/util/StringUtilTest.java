package com.github.bedrin.jdbc.sniffer.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {

    @Test
    public void testEscapeJsonString() throws Exception {
        assertEquals("\"\\\\\\\"\\t\\n\\r\"", StringUtil.escapeJsonString("\\\"\t\n\r"));
    }

}