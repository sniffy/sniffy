package io.sniffy.servlet;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import java.util.Enumeration;

/**
 * @see SniffyFilter
 * @since 2.3.0
 */
class FilterServletConfigAdapter implements ServletConfig {

    private final FilterConfig filterConfig;
    private final String servletName;

    public FilterServletConfigAdapter(FilterConfig filterConfig, String servletName) {
        this.filterConfig = filterConfig;
        this.servletName = servletName;
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return filterConfig.getServletContext();
    }

    @Override
    public String getInitParameter(String name) {
        return filterConfig.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return filterConfig.getInitParameterNames();
    }

}
