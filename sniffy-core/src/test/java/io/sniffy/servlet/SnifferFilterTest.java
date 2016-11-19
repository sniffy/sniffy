package io.sniffy.servlet;

import io.sniffy.BaseTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.sniffy.servlet.SnifferFilter.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SnifferFilterTest extends BaseTest {

    @Mock
    private FilterChain filterChain;

    private MockHttpServletResponse httpServletResponse;
    private MockServletContext servletContext;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletRequest httpServletRequest2;
    private MockHttpServletRequest httpServletRequest3;
    private MockHttpServletRequest httpServletRequest4;
    private SnifferFilter filter;

    @Before
    public void setupServletApiMocks() {
        httpServletResponse = new MockHttpServletResponse();
        servletContext = new MockServletContext("/petclinic");
        httpServletRequest = MockMvcRequestBuilders.get("/petclinic/foo/bar?baz").contextPath("/petclinic").buildRequest(servletContext);
        httpServletRequest2 = MockMvcRequestBuilders.get("/petclinic/foo/bar/").contextPath("/petclinic").buildRequest(servletContext);
        httpServletRequest3 = MockMvcRequestBuilders.get("/petclinic").contextPath("/petclinic").buildRequest(servletContext);
        httpServletRequest4 = MockMvcRequestBuilders.get("/petclinic/").contextPath("/petclinic").buildRequest(servletContext);
        filter = new SnifferFilter();
    }

    private FilterConfig getFilterConfig() {
        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameter("inject-html")).thenReturn("true");
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^/baz/.*$");
        ServletContext servletContext = mock(ServletContext.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getContextPath()).thenReturn("/petclinic");
        return filterConfig;
    }

    @Test
    public void testUnitializedFilter() throws IOException, ServletException {

        SnifferFilter filter = new SnifferFilter();
        filter.setInjectHtml(true);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertTrue(httpServletResponse.getHeaderNames().contains(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testFilterSniffyInjected() throws IOException, ServletException, ParserConfigurationException, SAXException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(httpServletResponse.getContentAsString().getBytes()));

        String sniffyJsSrc = doc.getElementsByTagName("script").item(0).getAttributes().getNamedItem("src").getNodeValue();

        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith("../" + SNIFFER_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("../" + REQUEST_URI_PREFIX));
    }

    private void answerWithContent(String actualContent) throws IOException, ServletException {
        doAnswer(invocation -> {
            Thread.sleep(1);
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.setContentType("text/html");
            PrintWriter printWriter = response.getWriter();
            printWriter.append(actualContent);
            Thread.sleep(1);
            return null;
        }).when(filterChain).doFilter(any(), any());
    }

    @Test
    public void testFilterSniffyInjected2() throws IOException, ServletException, ParserConfigurationException, SAXException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest2, httpServletResponse, filterChain);

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(httpServletResponse.getContentAsString().getBytes()));

        String sniffyJsSrc = doc.getElementsByTagName("script").item(0).getAttributes().getNamedItem("src").getNodeValue();

        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith("../../" + SNIFFER_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("../../" + REQUEST_URI_PREFIX));

    }

    @Test
    public void testFilterSniffyInjected3() throws IOException, ServletException, ParserConfigurationException, SAXException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest3, httpServletResponse, filterChain);

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(httpServletResponse.getContentAsString().getBytes()));

        Node scriptTag = doc.getElementsByTagName("script").item(0);
        assertNull(scriptTag.getAttributes().getNamedItem("src"));
        assertTrue(scriptTag.getTextContent().contains("document.write"));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("./" + REQUEST_URI_PREFIX));

    }

    @Test
    public void testFilterSniffyInjected4() throws IOException, ServletException, ParserConfigurationException, SAXException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest4, httpServletResponse, filterChain);

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(httpServletResponse.getContentAsString().getBytes()));

        String sniffyJsSrc = doc.getElementsByTagName("script").item(0).getAttributes().getNamedItem("src").getNodeValue();

        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith(SNIFFER_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith(REQUEST_URI_PREFIX));

    }

    @Test
    public void testExcludePattern() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^/foo/ba.*$");

        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.getHeaderNames().contains(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testDestroy() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^/foo/ba.*$");

        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        filter.destroy();

    }

    @Test
    public void testGetSnifferJs() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^.*(\\.js|\\.css)$");

        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        MockHttpServletRequest httpServletRequest = MockMvcRequestBuilders.
                get("/petclinic/" + SnifferFilter.JAVASCRIPT_URI).
                contextPath("/petclinic").buildRequest(servletContext);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.getHeaderNames().contains(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentLength() > 100);

    }

    @Test
    public void testGetSnifferJsMap() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^.*(\\.js|\\.css)$");

        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        MockHttpServletRequest httpServletRequest = MockMvcRequestBuilders.
                get("/petclinic/" + SnifferFilter.JAVASCRIPT_MAP_URI).
                contextPath("/petclinic").buildRequest(servletContext);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.getHeaderNames().contains(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentLength() > 100);

    }

    @Test
    public void testFilterNoQueries() throws IOException, ServletException {

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(0, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testFilterOneQuery() throws IOException, ServletException {

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));

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

        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testDisabledInConfigFilterOneQuery() throws IOException, ServletException {

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("enabled")).thenReturn("false");

        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testFilterEnabledByRequestParameter() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SnifferFilter filter = new SnifferFilter();
        filter.setEnabled(false);
        httpServletRequest.setParameter("sniffy", "true");
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertTrue(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
        assertEquals("Check cookie parameter specified", "true", httpServletResponse.getCookie("sniffy").getValue());
    }

    @Test
    public void testFilterNoCookies() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SnifferFilter filter = new SnifferFilter();
        filter.setEnabled(false);
        httpServletRequest.setCookies((Cookie[]) null);
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
    }

    @Test
    public void testFilterEnabledByCookie() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SnifferFilter filter = new SnifferFilter();
        filter.setEnabled(false);
        httpServletRequest.setCookies(new Cookie("sniffy", "true"));
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertTrue(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
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
        assertFalse("Filter must be disabled", httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
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
        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertArrayEquals("Hello, World".getBytes(), httpServletResponse.getContentAsByteArray());

    }

    @Test
    public void testFilterOneQueryCloseFlushes() throws IOException, ServletException {

        doExecuteQueryOnAnyRequest();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentAsString().substring(actualContent.length()).contains("id=\"sniffy\""));

    }

    @Test
    public void testInjectHtmlCharacterEncoding() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Привет, мир!</body></html>";
        final String cp1251 = "cp1251";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");
            response.setCharacterEncoding(cp1251);

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

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertTrue(
                -1 != Collections.indexOfSubList(
                        stream(httpServletResponse.getContentAsByteArray()).boxed().collect(Collectors.toList()),
                        stream("Привет, мир!".getBytes(cp1251)).boxed().collect(Collectors.toList())
                )
        );
        assertTrue(httpServletResponse.getContentAsString().substring(actualContent.length()).contains("id=\"sniffy\""));

    }

    public static IntStream stream(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return IntStream.generate(buffer::get).limit(buffer.remaining());
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

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        String contentAsString = httpServletResponse.getContentAsString();
        assertTrue(contentAsString.contains("id=\"sniffy\""));
        assertTrue(contentAsString.indexOf("id=\"sniffy\"") > contentAsString.indexOf("</body>"));
        assertTrue(contentAsString.contains("data-sql-queries=\"2\""));

    }

    @Test
    public void testInjectHtmlCloseResponse() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            printWriter.append(actualContent);
            printWriter.close();
            executeStatement();

            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        String contentAsString = httpServletResponse.getContentAsString();
        assertTrue(contentAsString.contains("id=\"sniffy\""));
        assertTrue(contentAsString.indexOf("id=\"sniffy\"") < contentAsString.indexOf("</body>"));
        assertTrue(contentAsString.contains("data-sql-queries=\"1\""));

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

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        String contentAsString = httpServletResponse.getContentAsString();
        assertTrue(contentAsString.substring(actualContent.length()).contains("id=\"sniffy\""));
        assertEquals(contentAsString.length(), httpServletResponse.getContentLength());
        assertTrue(httpServletResponse.getContentLength() > actualContent.length());

    }

    @Test
    public void testInjectHtmlSetContentLengthLong() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            response.setContentLengthLong(actualContent.length());
            printWriter.append(actualContent);
            executeStatement();
            printWriter.flush();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        String contentAsString = httpServletResponse.getContentAsString();
        assertTrue(contentAsString.substring(actualContent.length()).contains("id=\"sniffy\""));
        assertEquals(contentAsString.length(), httpServletResponse.getContentLength());
        assertTrue(httpServletResponse.getContentLength() > actualContent.length());

    }

    @Test
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

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        String contentAsString = httpServletResponse.getContentAsString();
        assertTrue(contentAsString.substring(actualContent.length()).contains("id=\"sniffy\""));
        assertEquals(contentAsString.length(), httpServletResponse.getContentLength());
        assertTrue(httpServletResponse.getContentLength() > actualContent.length());

    }

    @Test
    public void testInjectHtmlSetContentLengthAddIntHeader() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setContentType("text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            response.addIntHeader("Content-Length", 42);
            response.addIntHeader("Content-Length", actualContent.length());
            printWriter.append(actualContent);
            executeStatement();
            printWriter.flush();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
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

        assertEquals(4, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertEquals(250 * 1024, httpServletResponse.getContentAsByteArray().length);

    }

    @Test
    public void testFilterNoCorsHeaders() throws IOException, ServletException {

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(0, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains(HEADER_TIME_TO_FIRST_BYTE));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains(HEADER_REQUEST_DETAILS));

    }

    @Test
    public void testFilterAddCorsHeaders() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.addHeader(HEADER_CORS_HEADERS, "X-Custom-Header");
            response.flushBuffer();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(0, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains(HEADER_TIME_TO_FIRST_BYTE));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains(HEADER_REQUEST_DETAILS));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains("X-Custom-Header"));

    }

    @Test
    public void testFilterSetCorsHeaders() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.setHeader(HEADER_CORS_HEADERS, "X-Custom-Header-1");
            response.setHeader(HEADER_CORS_HEADERS, "X-Custom-Header-2");
            response.flushBuffer();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(0, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains(HEADER_TIME_TO_FIRST_BYTE));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains(HEADER_REQUEST_DETAILS));
        assertFalse(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains("X-Custom-Header-1"));
        assertTrue(httpServletResponse.getHeader(HEADER_CORS_HEADERS).contains("X-Custom-Header-2"));

    }

    @Test
    public void testFilterTimeToFirstByte() throws IOException, ServletException {

        doAnswer(invocation -> {
            Thread.sleep(1);
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.getOutputStream().write(1);
            response.flushBuffer();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertTrue(httpServletResponse.containsHeader(HEADER_TIME_TO_FIRST_BYTE));
        assertTrue(Integer.parseInt(httpServletResponse.getHeader(HEADER_TIME_TO_FIRST_BYTE)) >= 1);

    }

    @Test
    public void testFilterServerElapsedTime() throws IOException, ServletException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SnifferFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        Matcher matcher = Pattern.
                compile(".*data-server-time=\"(\\d+)\".*").
                matcher(httpServletResponse.getContentAsString());
        assertTrue(matcher.find());
        int serverTime = Integer.parseInt(matcher.group(1));

        assertTrue(serverTime >= 2);

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