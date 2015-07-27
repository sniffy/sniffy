package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SnifferFilterTest extends BaseTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private FilterChain filterChain;

    @Test
    public void testFilterNoQueries() throws IOException, ServletException {

        SnifferFilter filter = new SnifferFilter();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        verify(httpServletResponse).addIntHeader(SnifferFilter.HEADER_NAME, 0);

    }

    @Test
    public void testFilterOneQuery() throws IOException, ServletException {

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        SnifferFilter filter = new SnifferFilter();

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        verify(httpServletResponse).addIntHeader(SnifferFilter.HEADER_NAME, 1);

    }

    @Test
    public void testFilterOneQueryWithOutput() throws IOException, ServletException {

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

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

}