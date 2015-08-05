package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.sql.StatementMetaData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SnifferServletTest {

    private MockServletContext servletContext = new MockServletContext();
    private MockFilterConfig filterConfig = new MockFilterConfig(servletContext, "jdbc-sniffer");
    private ServletConfig servletConfig = new FilterServletConfigAdapter(filterConfig, "jdbc-sniffer");

    private Map<String, List<StatementMetaData>> cache;
    private SnifferServlet snifferServlet;

    @Before
    public void setupMocks() throws Exception {
        servletContext.setContextPath("/petclinic");
        cache = new HashMap<>();
        snifferServlet = new SnifferServlet(cache);
        snifferServlet.init(servletConfig);
    }

    @Test
    public void testGetJavascript() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.JAVASCRIPT_URI).
                buildRequest(servletContext);

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentLength() > 0);

    }

    @Test
    public void testGetCss() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.CSS_URI).
                buildRequest(servletContext);

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentLength() > 0);

    }

    @Test
    public void testGetRequest() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        cache.put("foo", Collections.singletonList(
                StatementMetaData.parse("SELECT 1 FROM DUAL", 300100999)
        ));

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentLength() > 0);
        assertEquals("[{\"query\":\"SELECT 1 FROM DUAL\",\"time\":300.101}]", response.getContentAsString());

    }

    @Test
    public void testGetRequestNotFound() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());

    }

    @Test
    public void testGetMissingResource() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic/foobar").
                buildRequest(servletContext);

        snifferServlet.service(request, response);

        assertFalse(response.isCommitted());

    }

    @Test
    public void testPostMissingResource() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                post("/petclinic/foobar").
                buildRequest(servletContext);

        snifferServlet.service(request, response);

        assertFalse(response.isCommitted());

    }

}