package io.sniffy.servlet;

import io.sniffy.Constants;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.LruCache;

import javax.servlet.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * HTTP Filter will capture the number of executed queries for given HTTP request and return it
 * as a 'X-Sql-Queries' header in response.
 *
 * It also can inject an icon with a number of executed queries to each HTML page
 * This feature is experimental and can be enabled using inject-html filter parameter
 *
 * Example of web.xml:
 * <pre>
 * {@code
 *   <filter>
 *        <filter-name>sniffer</filter-name>
 *        <filter-class>io.sniffy.servlet.SnifferFilter</filter-class>
 *        <init-param>
 *            <param-name>inject-html</param-name>
 *            <param-value>true</param-value>
 *        </init-param>
 *        <init-param>
 *            <param-name>enabled</param-name>
 *            <param-value>true</param-value>
 *        </init-param>
 *        <init-param>
 *            <param-name>exclude-pattern</param-name>
 *            <param-value>^/vets.html$</param-value>
 *        </init-param>
 *    </filter>
 *    <filter-mapping>
 *        <filter-name>sniffer</filter-name>
 *        <url-pattern>/*</url-pattern>
 *    </filter-mapping>
 * }
 * </pre>
 *
 * @since 2.3.0
 */
public class SnifferFilter implements Filter {

    public static final String HEADER_NUMBER_OF_QUERIES = "X-Sql-Queries";
    public static final String HEADER_REQUEST_DETAILS = "X-Request-Details";

    public static final String SNIFFER_URI_PREFIX =
            "/sniffy/" +
                    Constants.MAJOR_VERSION +
                    "." +
                    Constants.MINOR_VERSION +
                    "." +
                    Constants.PATCH_VERSION;

    public static final String JAVASCRIPT_URI = SNIFFER_URI_PREFIX + "/sniffy.min.js";
    public static final String REQUEST_URI_PREFIX = SNIFFER_URI_PREFIX + "/request/";

    protected boolean injectHtml = false;
    protected boolean enabled = true;
    protected Pattern excludePattern = null;

    // TODO: consider replacing with some concurrent collection instead
    protected final Map<String, List<StatementMetaData>> cache = Collections.synchronizedMap(
            new LruCache<String, List<StatementMetaData>>(10000)
    );

    protected SnifferServlet snifferServlet;
    protected ServletContext servletContext;

    protected String contextPath;

    public void init(FilterConfig filterConfig) throws ServletException {
        String injectHtml = filterConfig.getInitParameter("inject-html");
        if (null != injectHtml) {
            this.injectHtml = Boolean.parseBoolean(injectHtml);
        }
        String enabled = filterConfig.getInitParameter("enabled");
        if (null != enabled) {
            this.enabled = Boolean.parseBoolean(enabled);
        }
        String excludePattern = filterConfig.getInitParameter("exclude-pattern");
        if (null != excludePattern) {
            this.excludePattern = Pattern.compile(excludePattern);
        }

        snifferServlet = new SnifferServlet(cache);
        snifferServlet.init(new FilterServletConfigAdapter(filterConfig, "sniffy"));

        servletContext = filterConfig.getServletContext();
        contextPath = servletContext.getContextPath();

    }

    public void doFilter(final ServletRequest request, ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        // if disabled, run chain and return

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        // process Sniffy REST calls

        if (injectHtml) {
            try {
                snifferServlet.service(request, response);
                if (response.isCommitted()) return;
            } catch (Exception e) {
                servletContext.log("Exception in SniffyServlet; calling original chain", e);
                chain.doFilter(request, response);
                return;
            }
        }

        // create request decorator

        SniffyRequestProcessor sniffyRequestProcessor;
        try {
            sniffyRequestProcessor = new SniffyRequestProcessor(this, request, response);
        } catch (Exception e) {
            servletContext.log("Exception in SniffyRequestProcessor initialization; calling original chain", e);
            chain.doFilter(request, response);
            return;
        }

        sniffyRequestProcessor.process(chain);

    }

    public void destroy() {

    }

    public boolean isInjectHtml() {
        return injectHtml;
    }

    public void setInjectHtml(boolean injectHtml) {
        this.injectHtml = injectHtml;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
