package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SnifferFilterTest extends BaseTest {

    @Mock
    private FilterChain filterChain;

    @Test
    public void testFilterNoQueries() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

        SnifferFilter filter = new SnifferFilter();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(0, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NAME));

    }

    @Test
    public void testFilterOneQuery() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NAME));

    }

    @Test
    public void testFilterOneQueryWithOutput() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.getOutputStream().write("Hello, World".getBytes());
            executeStatement();
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NAME));
        assertArrayEquals("Hello, World".getBytes(), httpServletResponse.getContentAsByteArray());

    }

    @Test
    public void testFilterOneQueryWith100KOutputStream() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

        doAnswer(invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(new byte[50 * 1024]);
            executeStatement();
            outputStream.flush();
            outputStream.write(new byte[50 * 1024]);
            return null;
        }).when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NAME));
        assertEquals(100 * 1024, httpServletResponse.getContentAsByteArray().length);

    }

    @Test
    public void testFilterOneQueryWith100KPrintWriter() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

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

        SnifferFilter filter = new SnifferFilter();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(1, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NAME));
        assertEquals(100 * 1024, httpServletResponse.getContentAsByteArray().length);

    }

    @Test
    public void testInjectHtml() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

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

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameter("inject-html")).thenReturn("true");

        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NAME));
        assertTrue(httpServletResponse.getContentAsString().substring(actualContent.length()).contains("id=\"jdbc-sniffer-icon\""));

    }

    @Test
    public void testInjectHtmlSetContentLength() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

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

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameter("inject-html")).thenReturn("true");

        SnifferFilter filter = new SnifferFilter();
        filter.init(filterConfig);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(2, httpServletResponse.getHeaderValue(SnifferFilter.HEADER_NAME));
        assertTrue(httpServletResponse.getContentAsString().substring(actualContent.length()).contains("id=\"jdbc-sniffer-icon\""));
        assertTrue(httpServletResponse.getContentLength() > actualContent.length());

    }

}