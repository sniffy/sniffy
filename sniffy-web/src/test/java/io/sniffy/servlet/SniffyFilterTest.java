package io.sniffy.servlet;

import io.sniffy.BaseTest;
import io.sniffy.registry.ConnectionsRegistry;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
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

import static io.sniffy.servlet.SniffyFilter.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SniffyFilterTest extends BaseTest {

    private static class ApplicationSpecificException extends RuntimeException {}

    @Mock
    protected FilterChain filterChain;

    protected MockHttpServletResponse httpServletResponse;
    protected MockServletContext servletContext;
    protected MockHttpServletRequest requestWithPathAndQueryParameter;
    protected MockHttpServletRequest requestWithPath;
    protected MockHttpServletRequest requestContextWithoutTrailingSlash;
    protected MockHttpServletRequest requestContext;
    protected SniffyFilter filter;

    @Before
    public void setupServletApiMocks() {
        httpServletResponse = new MockHttpServletResponse();
        servletContext = new MockServletContext("/petclinic");
        requestWithPathAndQueryParameter = MockMvcRequestBuilders.get("/petclinic/foo/bar?baz").contextPath("/petclinic").buildRequest(servletContext);
        requestWithPath = MockMvcRequestBuilders.get("/petclinic/foo/bar/").contextPath("/petclinic").buildRequest(servletContext);
        requestContextWithoutTrailingSlash = MockMvcRequestBuilders.get("/petclinic").contextPath("/petclinic").buildRequest(servletContext);
        requestContext = MockMvcRequestBuilders.get("/petclinic/").contextPath("/petclinic").buildRequest(servletContext);
        filter = new SniffyFilter();
    }

    protected FilterConfig getFilterConfig() {
        return getFilterConfig(false);
    }

    private FilterConfig getFilterConfig(boolean faultToleranceCurrentRequest) {
        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameter("inject-html")).thenReturn("true");
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^/baz/.*$");
        when(filterConfig.getInitParameter("fault-tolerance-current-request")).thenReturn(Boolean.toString(faultToleranceCurrentRequest));
        ServletContext servletContext = mock(ServletContext.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getContextPath()).thenReturn("/petclinic");
        return filterConfig;
    }

    @Test
    public void testFailedFilterInitDisablesSniffy() throws ServletException {
        SniffyFilter filter = new SniffyFilter();
        filter.setEnabled(true);
        filter.init(null);
        assertFalse(filter.isEnabled());
    }

    @Test
    public void testThreadLocalConnectionRegistry() throws IOException, ServletException {

        ConnectionsRegistry.INSTANCE.setThreadLocal(false);
        try {

            SniffyFilter filter = new SniffyFilter();
            filter.init(getFilterConfig(true));

            assertTrue(ConnectionsRegistry.INSTANCE.isThreadLocal());
        } finally {
            ConnectionsRegistry.INSTANCE.setThreadLocal(false);
        }

    }

    @Test
    public void testThreadLocalConnectionRegistryUsesSession() throws IOException, ServletException {

        ConnectionsRegistry.INSTANCE.setThreadLocal(false);
        try {

            SniffyFilter filter = new SniffyFilter();
            filter.init(getFilterConfig(true));

            MockHttpServletRequest servletRequest = MockMvcRequestBuilders.
                    get("/petclinic/foo/bar?baz").
                    contextPath("/petclinic").
                    buildRequest(servletContext);

            filter.doFilter(servletRequest, httpServletResponse, filterChain);

            assertNotNull(servletRequest.getSession().getAttribute(THREAD_LOCAL_DISCOVERED_ADDRESSES));
            assertNotNull(servletRequest.getSession().getAttribute(THREAD_LOCAL_DISCOVERED_DATA_SOURCES));

        } finally {
            ConnectionsRegistry.INSTANCE.setThreadLocal(false);
        }

    }

    @Test
    public void testUnitializedFilter() throws IOException, ServletException {

        SniffyFilter filter = new SniffyFilter();
        filter.setInjectHtml(true);

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertTrue(httpServletResponse.getHeaderNames().contains(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testFilterSniffyInjected() throws IOException, ServletException, ParserConfigurationException, SAXException, ScriptException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        String sniffyJsSrc = extractSniffyJsSrc(httpServletResponse.getContentAsString());

        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith("../" + SNIFFY_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("../" + REQUEST_URI_PREFIX));
    }

    @Test
    @Issue("issues/275")
    public void testFilterRequestForwarded() throws IOException, ServletException, ParserConfigurationException, SAXException, ScriptException {

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        FilterChain outerFilterChain = mock(FilterChain.class);
        doAnswer(invocation -> {
            HttpServletRequest request = (HttpServletRequest) invocation.getArguments()[0];
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            filter.doFilter(request, response, filterChain);
            return null;
        }).when(outerFilterChain).doFilter(any(), any());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, outerFilterChain);

        String sniffyJsSrc = extractSniffyJsSrc(httpServletResponse.getContentAsString());
        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith("../" + SNIFFY_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("../" + REQUEST_URI_PREFIX));
    }

    @Test
    @Issue("issues/260")
    public void testFilterRequestErrorDispatch() throws IOException, ServletException, ParserConfigurationException, SAXException, ScriptException {

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        doAnswer(invocation -> {
            executeStatement();
            throw new ApplicationSpecificException();
            // TODO: test use case when first controller writes something to output buffer (with and without flushing)
            /*HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.setContentType("text/html");
            PrintWriter printWriter = response.getWriter();
            printWriter.append("<html><head><title>Title</title></head><body>Hello, World!</body></html>");
            return null;*/
        }).when(filterChain).doFilter(any(), any());

        try {
            filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);
            fail();
        } catch (ApplicationSpecificException e) {
            assertNotNull(e);
        }

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertEquals(1, Integer.parseInt(httpServletResponse.getHeader(HEADER_NUMBER_OF_QUERIES)));

        String sniffyJsSrc = extractSniffyJsSrc(httpServletResponse.getContentAsString());

        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith("../" + SNIFFY_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("../" + REQUEST_URI_PREFIX));
    }

    protected void answerWithContent(String actualContent) throws IOException, ServletException {
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

    public static class IoJS {

        private SniffyJS sniffy;

        public SniffyJS getSniffy() {
            return sniffy;
        }

        public void setSniffy(SniffyJS sniffy) {
            this.sniffy = sniffy;
        }

        public static class SniffyJS {}

    }

    public static class DocumentJS {

        private StringBuilder content = new StringBuilder();

        public void write(String content) {
            this.content.append(content);
        }

    }

    @Test
    public void testFilterSniffyInjectedRequestWithPath() throws IOException, ServletException, ParserConfigurationException, SAXException, ScriptException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPath, httpServletResponse, filterChain);

        String sniffyJsSrc = extractSniffyJsSrc(httpServletResponse.getContentAsString());

        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith("../../" + SNIFFY_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("../../" + REQUEST_URI_PREFIX));

    }

    @Test
    @Issue("issues/297")
    public void testFilterSniffyInjectedJustOnce() throws IOException, ServletException, ParserConfigurationException, SAXException, ScriptException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPath, httpServletResponse, filterChain);

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(httpServletResponse.getContentAsString().getBytes()));

        Node scriptTag = doc.getElementsByTagName("script").item(0);
        String scriptSource = scriptTag.getTextContent();
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("nashorn");
        ScriptContext scriptContext = engine.getContext();
        IoJS ioJS = new IoJS();
        ioJS.setSniffy(new IoJS.SniffyJS());
        scriptContext.setAttribute("io", ioJS, ScriptContext.ENGINE_SCOPE);
        DocumentJS documentJS = new DocumentJS();
        scriptContext.setAttribute("document", documentJS, ScriptContext.ENGINE_SCOPE);
        engine.eval(scriptSource);

        assertEquals(0, documentJS.content.length());

    }

    protected String extractSniffyJsSrc(String contentAsString) throws ParserConfigurationException, SAXException, IOException, ScriptException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(contentAsString.getBytes()));

        String sniffyJsSrc;

        Node scriptTag = doc.getElementsByTagName("script").item(0);
        if (scriptTag.hasAttributes() && null != scriptTag.getAttributes().getNamedItem("src")) {
            sniffyJsSrc = scriptTag.getAttributes().getNamedItem("src").getNodeValue();
        } else {
            String scriptSource = scriptTag.getTextContent();
            ScriptEngineManager engineManager = new ScriptEngineManager();
            ScriptEngine engine = engineManager.getEngineByName("nashorn");
            ScriptContext scriptContext = engine.getContext();
            IoJS ioJS = new IoJS();
            scriptContext.setAttribute("io", ioJS, ScriptContext.ENGINE_SCOPE);
            DocumentJS documentJS = new DocumentJS();
            scriptContext.setAttribute("document", documentJS, ScriptContext.ENGINE_SCOPE);
            engine.eval(scriptSource);

            assertTrue(documentJS.content.length() > 0);

            Document generatedDoc = documentBuilder.parse(new ByteArrayInputStream(documentJS.content.toString().getBytes()));
            sniffyJsSrc = generatedDoc.getElementsByTagName("script").item(0).getAttributes().getNamedItem("src").getNodeValue();
        }
        return sniffyJsSrc;
    }

    @Test
    public void testFilterSniffyInjectedContextWithoutTrailingSlash() throws IOException, ServletException, ParserConfigurationException, SAXException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestContextWithoutTrailingSlash, httpServletResponse, filterChain);

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(httpServletResponse.getContentAsString().getBytes()));

        Node scriptTag = doc.getElementsByTagName("script").item(0);
        assertNull(scriptTag.getAttributes().getNamedItem("src"));
        assertTrue(scriptTag.getTextContent().contains("document.write"));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("./" + REQUEST_URI_PREFIX));

    }

    @Test
    public void testFilterSniffyInjectedContext() throws IOException, ServletException, ParserConfigurationException, SAXException, ScriptException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestContext, httpServletResponse, filterChain);

        String sniffyJsSrc = extractSniffyJsSrc(httpServletResponse.getContentAsString());
        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith(SNIFFY_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith(REQUEST_URI_PREFIX));

    }

    @Test
    public void testExcludePattern() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^/foo/ba.*$");

        SniffyFilter filter = new SniffyFilter();
        filter.init(filterConfig);

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.getHeaderNames().contains(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    @Features("issues/304")
    public void testInjectHtmlExcludePattern() throws IOException, ServletException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("inject-html-exclude-pattern")).thenReturn("^/foo/ba.*$");

        SniffyFilter filter = new SniffyFilter();
        filter.init(filterConfig);

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertTrue(httpServletResponse.getHeaderNames().contains(HEADER_NUMBER_OF_QUERIES));
        assertFalse(httpServletResponse.getContentAsString().contains("script"));

    }

    @Test
    public void testDestroy() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^/foo/ba.*$");

        SniffyFilter filter = new SniffyFilter();
        filter.init(filterConfig);

        filter.destroy();

    }

    @Test
    public void testGetSnifferJs() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^.*(\\.js|\\.css)$");

        SniffyFilter filter = new SniffyFilter();
        filter.init(filterConfig);

        MockHttpServletRequest httpServletRequest = MockMvcRequestBuilders.
                get("/petclinic/" + JAVASCRIPT_URI).
                contextPath("/petclinic").buildRequest(servletContext);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.getHeaderNames().contains(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentLength() > 100);

    }

    @Test
    public void testGetSnifferJsMap() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        when(filterConfig.getInitParameter("exclude-pattern")).thenReturn("^.*(\\.js|\\.css)$");

        SniffyFilter filter = new SniffyFilter();
        filter.init(filterConfig);

        MockHttpServletRequest httpServletRequest = MockMvcRequestBuilders.
                get("/petclinic/" + JAVASCRIPT_MAP_URI).
                contextPath("/petclinic").buildRequest(servletContext);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.getHeaderNames().contains(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentLength() > 100);

    }

    @Test
    public void testFilterNoQueries() throws IOException, ServletException {

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertEquals(0, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testFilterOneQuery() throws IOException, ServletException {

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testFilterThrowsException() throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        SniffyFilter filter = new SniffyFilter();
        filter.init(filterConfig);

        doAnswer(invocation -> {throw new RuntimeException("test");}).
                when(filterChain).doFilter(any(), any());

        try {
            filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);
            fail();
        } catch (Exception e) {
            assertNotNull(e);
            assertEquals("test", e.getMessage());
        }

    }

    @Test
    public void testFilterOneQueryWithOutput() throws IOException, ServletException {

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.getOutputStream().write("Hello, World".getBytes());
            executeStatement();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertArrayEquals("Hello, World".getBytes(), httpServletResponse.getContentAsByteArray());

    }

    @Test
    public void testFilterOneQueryCloseFlushes() throws IOException, ServletException {

        doExecuteQueryOnAnyRequest();

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertArrayEquals("Hello, World".getBytes(), httpServletResponse.getContentAsByteArray());

    }

    @Test
    public void testFilterOneQueryTargetStreamClosed() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = spy(new MockHttpServletResponse());
        ServletOutputStream sos = mock(ServletOutputStream.class);
        when(httpServletResponse.getOutputStream()).thenReturn(sos);

        doExecuteQueryOnAnyRequest();

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertTrue(
                -1 != Collections.indexOfSubList(
                        stream(httpServletResponse.getContentAsByteArray()).boxed().collect(Collectors.toList()),
                        stream("Привет, мир!".getBytes(cp1251)).boxed().collect(Collectors.toList())
                )
        );
        assertTrue(httpServletResponse.getContentAsString().substring(actualContent.length()).contains("id=\"sniffy\""));

    }

    @Test
    public void testInjectHtmlSetContentTypeUsingHeader() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setHeader("Content-Type", "text/html");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            printWriter.append(actualContent);
            executeStatement();
            printWriter.flush();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertTrue(httpServletResponse.getContentAsString().substring(actualContent.length()).contains("id=\"sniffy\""));

    }

    @Test
    public void testInjectHtmlSetCharacterEncodingUsingHeader() throws IOException, ServletException {

        String actualContent = "<html><head><title>Title</title></head><body>Привет, мир!</body></html>";
        final String cp1251 = "cp1251";

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

            response.setHeader("Content-Type", "text/html; charset=cp1251");

            PrintWriter printWriter = response.getWriter();
            executeStatement();
            printWriter.append(actualContent);
            executeStatement();
            printWriter.flush();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        SniffyFilter filter = new SniffyFilter();

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertEquals(4, httpServletResponse.getHeaderValue(HEADER_NUMBER_OF_QUERIES));
        assertEquals(250 * 1024, httpServletResponse.getContentAsByteArray().length);

    }

    @Test
    public void testFilterNoCorsHeaders() throws IOException, ServletException {

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        assertTrue(httpServletResponse.containsHeader(HEADER_TIME_TO_FIRST_BYTE));
        assertTrue(Integer.parseInt(httpServletResponse.getHeader(HEADER_TIME_TO_FIRST_BYTE)) >= 1);

    }

    @Test
    public void testFilterServerElapsedTime() throws IOException, ServletException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SniffyFilter filter = new SniffyFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

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