package io.sniffy.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {

    @Test
    public void testEscapeJsonString() throws Exception {
        assertEquals("\"\\\\\\\"\\t\\n\\r\\f/_<\\/script>\"", StringUtil.escapeJsonString("\\\"\t\n\r\f/_</script>"));
        assertEquals("\"\"", StringUtil.escapeJsonString(null));
    }

}