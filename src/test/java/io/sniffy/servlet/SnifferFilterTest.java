package io.sniffy.servlet;

import io.sniffy.BaseTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SnifferFilterTest extends BaseTest {

    @Mock
    private FilterChain filterChain;

    private MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
    private MockServletContext servletContext = new MockServletContext("/petclinic/");
    private MockHttpServletRequest httpServletRequest =
            MockMvcRequestBuilders.get("/petclinic/foo/bar?baz").contextPath("/petclinic").buildRequest(servletContext);
    private SnifferFilter filter = new SnifferFilter();

    protected FilterConfig getFilterConfig() {
        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameter("inject-html")).thenReturn("true");
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^/baz/.*$");
        ServletContext servletContext = mock(ServletContext.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        return filterConfig;
    }

    @Test
    public void testExcludePattern() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^/foo/ba.*$");

        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.getHeaderNames().contains(SnifferFilter.HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testGetSnifferJs() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^.*(\\.js|\\.css)$");

        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        MockHttpServletRequest httpServletRequest = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.JAVASCRIPT_URI).
                contextPath("/petclinic").buildRequest(servletContext);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.getHeaderNames().contains(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentLength() > 100);

    }

    @Test
    public void testFilterNoQueries() throws IOException, ServletException {

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(0, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testFilterOneQuery() throws IOException, ServletException {

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testFilterThrowsException() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        doAnswer(invocation -> {throw new RuntimeException("test");}).
                when(filterChain).doFilter(any(), any());

        try {
            filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            fail();
        } catch (Exception e) {
            assertNotNull(e);
            assertEquals("test", e.getMessage());
        }

    }


    @Test
    public void testDisabledFilterOneQuery() throws IOException, ServletException {

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.setEnabled(false);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.containsHeader(SnifferFilter.HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testFilterEnabledByRequestParameter() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SnifferFilter filter = new SnifferFilter();
        filter.setEnabled(false);
        httpServletRequest.setParameter("sniffy", "true");
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertTrue(httpServletResponse.containsHeader(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertEquals("Check cookie parameter specified", "true", httpServletResponse.getCookie("sniffy").getValue());
    }

    @Test
    public void testFilterEnabledByCookie() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SnifferFilter filter = new SnifferFilter();
        filter.setEnabled(false);
        httpServletRequest.setCookies(new Cookie("sniffy", "true"));
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertTrue(httpServletResponse.containsHeader(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
    }

    @Test
    public void testFilterEnabledRequestParamOverridesCookie() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SnifferFilter filter = new SnifferFilter();
        filter.setEnabled(false);
        httpServletRequest.setParameter("sniffy", "false");
        httpServletRequest.setCookies(new Cookie("sniffy", "true"));
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertFalse("Filter must be disabled", httpServletResponse.containsHeader(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertEquals("Cookie parameter must be replaced", "false", httpServletResponse.getCookie("sniffy").getValue());
    }

    @Test
    public void testFilterDisabledByRequestParameter() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SnifferFilter filter = new SnifferFilter();
        filter.setEnabled(true);
        httpServletRequest.setParameter("sniffy", "false");
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertFalse(httpServletResponse.containsHeader(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
    }

    @Test
    public void testFilterOneQueryWithOutput() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.getOutputStream().write("Hello, World".getBytes());
            executeStatement();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertArrayEquals("Hello, World".getBytes(), httpServletResponse.getContentAsByteArray());

    }

    @Test
    public void testFilterOneQueryCloseFlushes() throws IOException, ServletException {

        doExecuteQueryOnAnyRequest();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertArrayEquals("Hello, World".getBytes(), httpServletResponse.getContentAsByteArray());

    }

    @Test
    public void testFilterOneQueryTargetStreamClosed() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = spy(new MockHttpServletResponse());
        ServletOutputStream sos = mock(ServletOutputStream.class);
        when(httpServletResponse.getOutputStream()).thenReturn(sos);

        doExecuteQueryOnAnyRequest();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        verify(sos).close();

    }

    @Test
    public void testFilterOneQueryWith100KOutputStream() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(new byte[50 * 1024]);
            executeStatement();
            outputStream.flush();
            outputStream.write(new byte[50 * 1024]);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertEquals(100 * 1024, httpServletResponse.getContentAsByteArray().length);

    }

    @Test
    public void testFilterOneQueryWith100KPrintWriter() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            PrintWriter printWriter = response.getWriter();

            StringBuilder sb = new StringBuilder();
            for (int i = 0 ; i < 1024; i++) {
                sb.append("<sometag>abcdef</sometag>");
            }

            String content = sb.toString();
            assertEquals(25 * 1024, content.getBytes().length);

            printWriter.write(content);
            printWriter.write(content);
            executeStatement();
            printWriter.flush();
            printWriter.write(content);
            printWriter.write(content);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertEquals(100 * 1024, httpServletResponse.getContentAsByteArray().length);

    }

    @Test
    public void testInjectHtml() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            printWriter.append(actualContent);
            executeStatement();
            printWriter.flush();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentAsString().substring(actualContent.length()).contains("id=\"sniffy\""));

    }

    @Test
    public void testDoNotInjectToXml() throws IOException, ServletException {

        String actualContent = "<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<applications location=\"PRODEXTSG\" hasPendingActions=\"false\">\n" +
                "    <application>" +
                "</application>\n" +
                "    <featuredItems>\n" +
                "        \n" +
                "    </featuredItems>\n" +
                "</applications>\n";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            printWriter.append(actualContent);
            executeStatement();
            printWriter.flush();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertEquals(actualContent, httpServletResponse.getContentAsString());
        assertFalse(httpServletResponse.getContentAsString().contains("id=\"sniffy\""));

    }

    @Test
    public void testInjectHtmlFlushResponse() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            printWriter.append(actualContent);
            response.flushBuffer();
            executeStatement();

            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        String contentAsString = httpServletResponse.getContentAsString();
        assertTrue(contentAsString.contains("id=\"sniffy\""));
        assertTrue(contentAsString.indexOf("id=\"sniffy\"") < contentAsString.indexOf("</body>"));
        assertTrue(contentAsString.contains("data-sql-queries=\"2\""));

    }

    @Test
    public void testInjectHtmlSetContentLength() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            response.setContentLength(actualContent.length());
            printWriter.append(actualContent);
            executeStatement();
            printWriter.flush();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        String contentAsString = httpServletResponse.getContentAsString();
        assertTrue(contentAsString.substring(actualContent.length()).contains("id=\"sniffy\""));
        assertEquals(contentAsString.length(), httpServletResponse.getContentLength());
        assertTrue(httpServletResponse.getContentLength() > actualContent.length());

    }

    @Test
    public void testInjectHtmlSetContentLengthHeader() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            response.setHeader("Content-Length", Integer.toString(actualContent.length()));
            printWriter.append(actualContent);
            executeStatement();
            printWriter.flush();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        String contentAsString = httpServletResponse.getContentAsString();
        assertTrue(contentAsString.substring(actualContent.length()).contains("id=\"sniffy\""));
        assertEquals(contentAsString.length(), httpServletResponse.getContentLength());
        assertTrue(httpServletResponse.getContentLength() > actualContent.length());

    }

    @Test
    @Ignore("spring test framework bug")
    public void testInjectHtmlSetContentLengthIntHeader() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            response.setIntHeader("Content-Length", actualContent.length());
            printWriter.append(actualContent);
            executeStatement();
            printWriter.flush();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        String contentAsString = httpServletResponse.getContentAsString();
        assertTrue(contentAsString.substring(actualContent.length()).contains("id=\"sniffy\""));
        assertEquals(contentAsString.length(), httpServletResponse.getContentLength());
        assertTrue(httpServletResponse.getContentLength() > actualContent.length());

    }

    @Test
    public void testFilterOneQuerySendError() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            executeStatement();
            response.sendError(HttpServletResponse.SC_CONFLICT);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertEquals(HttpServletResponse.SC_CONFLICT, httpServletResponse.getStatus());

    }

    @Test
    public void testFilterOneQuerySendErrorWithBody() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            executeStatement();
            response.setContentType("text/html");
            response.sendError(HttpServletResponse.SC_CONFLICT, "Body");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertEquals(HttpServletResponse.SC_CONFLICT, httpServletResponse.getStatus());

    }

    @Test
    public void testFilterOneQuerySendRedirect() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            executeStatement();
            response.sendRedirect("http://www.google.com/");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, httpServletResponse.getStatus());
        assertEquals("http://www.google.com/", httpServletResponse.getHeader("Location"));

    }

    @Test
    public void testFilterOneQueryResetResponseBuffer() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            executeStatement();

            PrintWriter printWriter = response.getWriter();
            printWriter.append("<div>content</div>");
            response.resetBuffer();
            response.setContentType("text/html");
            printWriter.append(actualContent);

            executeStatement();

            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentAsString().substring(actualContent.length()).contains("id=\"sniffy\""));

    }

    @Test
    public void testFilterOneQueryResetResponse() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            executeStatement();

            PrintWriter printWriter = response.getWriter();
            printWriter.append("<div>content</div>");
            response.reset();
            response.setContentType("text/html");
            printWriter.append(actualContent);

            executeStatement();

            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentAsString().substring(actualContent.length()).contains("id=\"sniffy\""));

    }

    @Test
    public void testFilterBufferSize() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.setBufferSize(200 * 1024);
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(new byte[50 * 1024]);
            executeStatement();
            outputStream.write(new byte[50 * 1024]);
            executeStatement();
            outputStream.write(new byte[50 * 1024]);
            executeStatement();
            outputStream.write(new byte[50 * 1024]);
            executeStatement();
            // response will be flushed here
            outputStream.write(new byte[50 * 1024]);
            executeStatement();
            assertEquals(200*1024, response.getBufferSize());
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(4, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NUMBER_OF_QUERIES));
        assertEquals(250 * 1024, httpServletResponse.getContentAsByteArray().length);

    }

    private void doExecuteQueryOnAnyRequest() throws IOException, ServletException {
        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write("Hello, World".getBytes());
            executeStatement();
            outputStream.close();
            return null;
        }).when(filterChain).doFilter(any(), any());
    }

}