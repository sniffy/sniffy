package io.sniffy.servlet;

import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.socket.SocketsRegistry;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SnifferServletTest {

    private MockServletContext servletContext = new MockServletContext();
    private MockFilterConfig filterConfig = new MockFilterConfig(servletContext, "sniffy");
    private ServletConfig servletConfig = new FilterServletConfigAdapter(filterConfig, "sniffy");

    private Map<String, RequestStats> cache;
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
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentLength() > 0);

    }

    @Test
    public void testGetSocketRegistry() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferServlet.SOCKET_REGISTRY_URI_PREFIX).
                buildRequest(servletContext);

        request.setContextPath("/petclinic");

        SocketsRegistry.INSTANCE.setSocketAddressStatus("localhost", SocketsRegistry.SocketAddressStatus.OPEN);

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentAsByteArray().length > 0);
        //assertTrue(response.getContentLength() > 0);

        SocketsRegistry.INSTANCE.clear();

    }

    @Test
    public void testGetRequest() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        cache.put("foo", new RequestStats(21, 42, Collections.singletonMap(
                new StatementMetaData(
                        "SELECT 1 FROM DUAL",
                        StatementMetaData.guessQueryType("SELECT 1 FROM DUAL"),
                        new AsyncResult<String>(""),
                        Thread.currentThread().getId()
                ), new SqlStats(300999, 0, 0, 0, 1))
        ));

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentLength() > 0);
        assertEquals("{\"timeToFirstByte\":21,\"time\":42,\"executedQueries\":[{\"query\":\"SELECT 1 FROM DUAL\",\"stackTrace\":\"\",\"time\":300.999,\"invocations\":1,\"rows\":0,\"type\":\"SELECT\",\"bytesDown\":0,\"bytesUp\":0}]}", response.getContentAsString());

    }

    @Test
    public void testGetRequestWithNetworkConnections() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        cache.put("foo", new RequestStats(
                        21,
                        42,
                        Collections.singletonMap(
                                new StatementMetaData(
                                        "SELECT 1 FROM DUAL",
                                        StatementMetaData.guessQueryType("SELECT 1 FROM DUAL"),
                                        new AsyncResult<String>(""),
                                        Thread.currentThread().getId()
                                ), new SqlStats(300999, 200, 300, 0, 1)),
                        Collections.singletonMap(
                                new SocketMetaData(
                                        new InetSocketAddress(InetAddress.getLocalHost(), 5555),
                                        42,
                                        new AsyncResult<String>("stackTrace"),
                                        Thread.currentThread().getId()
                                ),
                                new SocketStats(100, 200, 300)
                        )
                )
        );

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentLength() > 0);
        assertEquals("{\"timeToFirstByte\":21,\"time\":42," +
                "\"executedQueries\":[{\"query\":\"SELECT 1 FROM DUAL\",\"stackTrace\":\"\",\"time\":300.999,\"invocations\":1,\"rows\":0,\"type\":\"SELECT\",\"bytesDown\":200,\"bytesUp\":300}]," +
                "\"networkConnections\":[{\"host\":\"" + InetAddress.getLocalHost().toString() + ":5555\",\"stackTrace\":\"stackTrace\",\"time\":100.000,\"bytesDown\":200,\"bytesUp\":300}]" +
                "}", response.getContentAsString());

    }

    @Test
    public void testGetComplexRequest() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        cache.put("foo", new RequestStats(21, 42, Collections.singletonMap(
                new StatementMetaData(
                        "SELECT \r\n\"1\" FROM 'DUAL'",
                        StatementMetaData.guessQueryType("SELECT \r\n\"1\" FROM 'DUAL'"),
                        new AsyncResult<String>("io.sniffy.Test.method(Test.java:99)"),
                        Thread.currentThread().getId()
                ), new SqlStats(300999, 0, 0, 0, 1))
        ));

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentLength() > 0);
        assertEquals("{\"timeToFirstByte\":21,\"time\":42,\"executedQueries\":[{\"query\":\"SELECT \\r\\n\\\"1\\\" FROM 'DUAL'\",\"stackTrace\":\"io.sniffy.Test.method(Test.java:99)\",\"time\":300.999,\"invocations\":1,\"rows\":0,\"type\":\"SELECT\",\"bytesDown\":0,\"bytesUp\":0}]}", response.getContentAsString());

    }

    @Test
    public void testGetRequestNotFound() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals(0, response.getContentLength());
        assertEquals(0, response.getContentLength());

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