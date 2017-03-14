package io.sniffy.servlet;

import io.sniffy.BaseTest;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.yandex.qatools.allure.annotations.Features;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;

import static io.sniffy.servlet.SniffyFilter.HEADER_NUMBER_OF_QUERIES;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SniffyFilterConfigurationTest extends BaseTest {

    @Mock
    protected FilterChain filterChain;

    protected MockServletContext servletContext;
    protected MockHttpServletRequest httpServletRequest;
    protected MockHttpServletResponse httpServletResponse;

    @Before
    public void setupServletApiMocks() {
        httpServletResponse = new MockHttpServletResponse();
        servletContext = new MockServletContext("/petclinic");
        httpServletRequest = MockMvcRequestBuilders.get("/petclinic/foo/bar?baz").contextPath("/petclinic").buildRequest(servletContext);
    }


    @Test
    public void testDisabledFilterOneQuery() throws IOException, ServletException {

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        SniffyFilter filter = new SniffyFilter();
        filter.setEnabled(false);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    public void testDisabledInConfigFilterOneQuery() throws IOException, ServletException {

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameter("enabled")).thenReturn("false");
        when(filterConfig.getServletContext()).thenReturn(servletContext);

        SniffyFilter filter = new SniffyFilter();
        filter.init(filterConfig);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    @Features("issues/288")
    public void testSystemInConfigFilterDisabledGloballyOneQuery() throws IOException, ServletException {

        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameter("enabled")).thenReturn("system");
        when(filterConfig.getServletContext()).thenReturn(servletContext);

        SniffyFilter filter = new SniffyFilter();
        filter.init(filterConfig);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));

    }

    @Test
    @Features("issues/288")
    public void testSystemInConfigFilterEnabledGloballyOneQuery() throws IOException, ServletException {

        Boolean filterEnabledBackup = SniffyConfiguration.INSTANCE.getFilterEnabled();
        try {
            SniffyConfiguration.INSTANCE.setFilterEnabled(true);

            doAnswer(invocation -> {
                executeStatement();
                return null;
            }).
                    when(filterChain).doFilter(any(), any());

            FilterConfig filterConfig = mock(FilterConfig.class);
            when(filterConfig.getInitParameter("enabled")).thenReturn("system");
            when(filterConfig.getServletContext()).thenReturn(servletContext);

            SniffyFilter filter = new SniffyFilter();
            filter.init(filterConfig);

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

            assertTrue(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
        } finally {
            SniffyConfiguration.INSTANCE.setFilterEnabled(filterEnabledBackup);
        }

    }

    @Test
    public void testFilterEnabledByRequestParameter() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SniffyFilter filter = new SniffyFilter();
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
        SniffyFilter filter = new SniffyFilter();
        filter.setEnabled(false);
        httpServletRequest.setCookies((Cookie[]) null);
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
    }

    @Test
    public void testFilterEnabledByCookie() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SniffyFilter filter = new SniffyFilter();
        filter.setEnabled(false);
        httpServletRequest.setCookies(new Cookie("sniffy", "true"));
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertTrue(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
    }

    @Test
    public void testFilterEnabledByHeader() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SniffyFilter filter = new SniffyFilter();
        filter.setEnabled(false);
        httpServletRequest.addHeader("Sniffy-Enabled", "true");
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertTrue(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
    }

    @Test
    public void testFilterDisabledByHeader() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SniffyFilter filter = new SniffyFilter();
        filter.setEnabled(true);
        httpServletRequest.addHeader("Sniffy-Enabled", "false");
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
    }

    @Test
    public void testFilterEnabledRequestParamOverridesCookie() throws IOException, ServletException {
        doAnswer(invocation -> {executeStatement(); return null;}).
                when(filterChain).doFilter(any(), any());
        SniffyFilter filter = new SniffyFilter();
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
        SniffyFilter filter = new SniffyFilter();
        filter.setEnabled(true);
        httpServletRequest.setParameter("sniffy", "false");
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertFalse(httpServletResponse.containsHeader(HEADER_NUMBER_OF_QUERIES));
    }

}
