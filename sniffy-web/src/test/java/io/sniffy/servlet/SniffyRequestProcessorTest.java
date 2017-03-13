package io.sniffy.servlet;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Issue;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SniffyRequestProcessorTest {

    @Test
    @Issue("issues/272")
    public void testGetBestRelativeURIForPathMapping() {

        HttpServletRequest req = mock(HttpServletRequest.class);

        when(req.getRequestURI()).thenReturn("/application/bar/baz");
        when(req.getContextPath()).thenReturn("/application");
        when(req.getServletPath()).thenReturn("/bar");
        when(req.getPathInfo()).thenReturn("/baz");

        ServletContext sc = mock(ServletContext.class);
        when(req.getServletContext()).thenReturn(sc);

        Map<String,ServletRegistration> servletRegistrations = new HashMap<>();
        when(sc.getServletRegistrations()).thenAnswer(inv -> servletRegistrations);

        ServletRegistration sr = mock(ServletRegistration.class);
        when(sr.getMappings()).thenReturn(Collections.singletonList("/bar/*"));
        servletRegistrations.put("ServletName", sr);

        assertEquals("/baz", SniffyRequestProcessor.getBestRelativeURI(req));

    }

    @Test
    @Issue("issues/272")
    public void testGetBestRelativeURIForPathMappingWithoutTrailingSlash() {

        HttpServletRequest req = mock(HttpServletRequest.class);

        when(req.getRequestURI()).thenReturn("/application/bar");
        when(req.getContextPath()).thenReturn("/application");
        when(req.getServletPath()).thenReturn("/bar");
        when(req.getPathInfo()).thenReturn(null);

        ServletContext sc = mock(ServletContext.class);
        when(req.getServletContext()).thenReturn(sc);

        Map<String,ServletRegistration> servletRegistrations = new HashMap<>();
        when(sc.getServletRegistrations()).thenAnswer(inv -> servletRegistrations);

        ServletRegistration sr = mock(ServletRegistration.class);
        when(sr.getMappings()).thenReturn(Collections.singletonList("/bar/*"));
        servletRegistrations.put("ServletName", sr);

        assertEquals("", SniffyRequestProcessor.getBestRelativeURI(req));

    }

    @Test
    @Issue("issues/272")
    public void testGetBestRelativeURIForExactMatch() {

        HttpServletRequest req = mock(HttpServletRequest.class);

        when(req.getRequestURI()).thenReturn("/application/bar/");
        when(req.getContextPath()).thenReturn("/application");
        when(req.getServletPath()).thenReturn("/bar/");
        when(req.getPathInfo()).thenReturn(null);

        ServletContext sc = mock(ServletContext.class);
        when(req.getServletContext()).thenReturn(sc);

        Map<String,ServletRegistration> servletRegistrations = new HashMap<>();
        when(sc.getServletRegistrations()).thenAnswer(inv -> servletRegistrations);

        ServletRegistration sr = mock(ServletRegistration.class);
        when(sr.getMappings()).thenReturn(Collections.singletonList("/bar/"));
        servletRegistrations.put("ServletName", sr);

        assertEquals("/", SniffyRequestProcessor.getBestRelativeURI(req));

    }

    @Test
    @Issue("issues/272")
    public void testGetBestRelativeURIForExtensionMapping() {

        HttpServletRequest req = mock(HttpServletRequest.class);

        when(req.getRequestURI()).thenReturn("/application/bar/baz.do");
        when(req.getContextPath()).thenReturn("/application");
        when(req.getServletPath()).thenReturn("/bar/baz.do");
        when(req.getPathInfo()).thenReturn(null);

        ServletContext sc = mock(ServletContext.class);
        when(req.getServletContext()).thenReturn(sc);

        Map<String,ServletRegistration> servletRegistrations = new HashMap<>();
        when(sc.getServletRegistrations()).thenAnswer(inv -> servletRegistrations);

        ServletRegistration sr = mock(ServletRegistration.class);
        when(sr.getMappings()).thenReturn(Collections.singletonList("*.do"));
        servletRegistrations.put("ServletName", sr);

        assertEquals("/bar/baz.do", SniffyRequestProcessor.getBestRelativeURI(req));

    }

}
