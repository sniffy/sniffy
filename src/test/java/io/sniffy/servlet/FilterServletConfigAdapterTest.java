package io.sniffy.servlet;

import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;

import java.util.Enumeration;

import static org.junit.Assert.*;

public class FilterServletConfigAdapterTest {

    @Test
    public void testAdapter() throws Exception {

        MockFilterConfig mockFilterConfig = new MockFilterConfig();
        mockFilterConfig.addInitParameter("foo", "bar");

        FilterServletConfigAdapter adapter = new FilterServletConfigAdapter(mockFilterConfig, "my-servlet");

        assertEquals("my-servlet", adapter.getServletName());
        assertEquals(mockFilterConfig.getServletContext(), adapter.getServletContext());
        assertEquals("bar", adapter.getInitParameter("foo"));

        Enumeration<String> initParameterNames = adapter.getInitParameterNames();
        assertNotNull(initParameterNames);
        assertTrue(initParameterNames.hasMoreElements());
        assertEquals("foo", initParameterNames.nextElement());
        assertFalse(initParameterNames.hasMoreElements());

    }

}