package io.sniffy.servlet;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.sniffy.Constants;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
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

    public static final String HEADER_CORS_HEADERS = "Access-Control-Expose-Headers";
    public static final String HEADER_NUMBER_OF_QUERIES = "X-Sql-Queries";
    public static final String HEADER_TIME_TO_FIRST_BYTE = "X-Time-To-First-Byte";
    public static final String HEADER_REQUEST_DETAILS = "X-Request-Details";

    public static final String SNIFFER_URI_PREFIX =
            "/sniffy/" +
                    Constants.MAJOR_VERSION +
                    "." +
                    Constants.MINOR_VERSION +
                    "." +
                    Constants.PATCH_VERSION;

    public static final String JAVASCRIPT_URI = SNIFFER_URI_PREFIX + "/sniffy.min.js";
    public static final String JAVASCRIPT_MAP_URI = SNIFFER_URI_PREFIX + "/sniffy.map";
    public static final String REQUEST_URI_PREFIX = SNIFFER_URI_PREFIX + "/request/";
    public static final String SNIFFY = "sniffy";

    protected boolean injectHtml = false;
    protected boolean enabled = true;
    protected Pattern excludePattern = null;

    protected final Map<String, RequestStats> cache = new ConcurrentLinkedHashMap.Builder<String, RequestStats>().
                    maximumWeightedCapacity(200).
                    build();

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

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        Boolean sniffyEnabled = enabled;

        // override by request parameter/cookie value if provided

        String sniffyEnabledParam = request.getParameter(SNIFFY);

        if (null != sniffyEnabledParam) {
            sniffyEnabled = Boolean.parseBoolean(sniffyEnabledParam);
            setSessionCookie(httpServletResponse, SNIFFY, String.valueOf(sniffyEnabled));
        } else {
            sniffyEnabledParam = readCookie(httpServletRequest, SNIFFY);
            if (null != sniffyEnabledParam) {
                sniffyEnabled = Boolean.parseBoolean(sniffyEnabledParam);
            }
        }

        // if disabled, run chain and return

        if (!sniffyEnabled) {
            chain.doFilter(request, response);
            return;
        }

        // process Sniffy REST calls

        if (injectHtml && null != snifferServlet) {
            try {
                snifferServlet.service(request, response);
                if (response.isCommitted()) return;
            } catch (Exception e) {
                if (null != servletContext) servletContext.log("Exception in SniffyServlet; calling original chain", e);
                chain.doFilter(request, response);
                return;
            }
        }

        // create request decorator

        SniffyRequestProcessor sniffyRequestProcessor;
        try {
            sniffyRequestProcessor = new SniffyRequestProcessor(this, httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            if (null != servletContext) servletContext.log("Exception in SniffyRequestProcessor initialization; calling original chain", e);
            chain.doFilter(request, response);
            return;
        }

        sniffyRequestProcessor.process(chain);

    }

    private void setSessionCookie(HttpServletResponse httpServletResponse,
                                  String name, String value) throws MalformedURLException {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        httpServletResponse.addCookie(cookie);
    }

    private String readCookie(HttpServletRequest httpServletRequest, String name) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (null != cookies) for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
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
