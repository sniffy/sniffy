package io.sniffy.servlet;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SniffyServletResourceTest {

    @Test
    public void loadsAllSharedProfilerResources() throws Exception {
        SniffyServlet servlet = new SniffyServlet(new HashMap<String, RequestStats>());

        assertNotNull(servlet.javascript);
        assertNotNull(servlet.javascriptSource);
        assertNotNull(servlet.javascriptMap);
        assertTrue(new String(servlet.javascript, "UTF-8").contains("sourceMappingURL=sniffy.map"));
        assertTrue(new String(servlet.javascriptSource, "UTF-8").contains("sniffy-profiler"));
        assertTrue(servlet.javascriptMap.length > 0);
    }

}
