package io.sniffy.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtilTest {

    @Test
    public void testEscapeJsonString() throws Exception {
        assertEquals("\"\\u0001\\b\\\\\\\"\\t\\n\\r\\f/_<\\/script>\"", StringUtil.escapeJsonString("\u0001\b\\\"\t\n\r\f/_</script>"));
        assertEquals("\"\"", StringUtil.escapeJsonString(null));
    }

}