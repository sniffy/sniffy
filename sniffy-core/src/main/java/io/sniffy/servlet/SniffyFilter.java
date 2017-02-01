package io.sniffy.servlet;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.sniffy.Constants;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * HTTP Filter will capture the number of executed queries for given HTTP request and return it
 * as a 'Sniffy-Sql-Queries' header in response.
 *
 * It also can inject an icon with a number of executed queries to each HTML page
 * This feature is experimental and can be enabled using inject-html filter parameter
 *
 * Example of web.xml:
 * <pre>
 * {@code
 *   <filter>
 *        <filter-name>sniffer</filter-name>
 *        <filter-class>io.sniffy.servlet.SniffyFilter</filter-class>
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
public class SniffyFilter implements Filter {

    public static final String HEADER_CORS_HEADERS = "Access-Control-Expose-Headers";
    public static final String HEADER_NUMBER_OF_QUERIES = "Sniffy-Sql-Queries";
    public static final String HEADER_TIME_TO_FIRST_BYTE = "Sniffy-Time-To-First-Byte";
    public static final String HEADER_REQUEST_DETAILS = "Sniffy-Request-Details";

    public static final String SNIFFER_URI_PREFIX =
            "sniffy/" +
                    Constants.MAJOR_VERSION +
                    "." +
                    Constants.MINOR_VERSION +
                    "." +
                    Constants.PATCH_VERSION;

    public static final String JAVASCRIPT_URI = SNIFFER_URI_PREFIX + "/sniffy.min.js";
    public static final String JAVASCRIPT_MAP_URI = SNIFFER_URI_PREFIX + "/sniffy.map";
    public static final String REQUEST_URI_PREFIX = SNIFFER_URI_PREFIX + "/request/";
    public static final String SNIFFY = "sniffy";
    protected static final String THREAD_LOCAL_DISCOVERED_ADDRESSES = "discoveredAddresses";
    protected static final String THREAD_LOCAL_DISCOVERED_DATA_SOURCES = "discoveredDataSources";

    protected boolean injectHtml = false;
    protected boolean enabled = true;
    protected Pattern excludePattern = null;

    protected final Map<String, RequestStats> cache = new ConcurrentLinkedHashMap.Builder<String, RequestStats>().
            maximumWeightedCapacity(200).
            build();

    protected SniffyServlet sniffyServlet = new SniffyServlet(cache);
    protected ServletContext servletContext; // TODO: log via slf4j if available

    public SniffyFilter() {
        enabled = SniffyConfiguration.INSTANCE.isFilterEnabled();
        injectHtml = SniffyConfiguration.INSTANCE.isInjectHtml();
        try {
            String excludePattern = SniffyConfiguration.INSTANCE.getExcludePattern();
            if (null != excludePattern) {
                this.excludePattern = Pattern.compile(excludePattern);
            }
        } catch (PatternSyntaxException e) {
            // TODO: log me maybe?
        }
    }

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
            this.excludePattern = Pattern.compile(excludePattern); // TODO: can throw exception
        }

        String faultToleranceCurrentRequest = filterConfig.getInitParameter("fault-tolerance-current-request");
        if (null != faultToleranceCurrentRequest) {
            ConnectionsRegistry.INSTANCE.setThreadLocal(Boolean.parseBoolean(faultToleranceCurrentRequest));
        }

        sniffyServlet.init(new FilterServletConfigAdapter(filterConfig, "sniffy"));

        servletContext = filterConfig.getServletContext();

    }

    public void doFilter(final ServletRequest request, ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        // if disabled, run chain and return
        if (!isSniffyFilterEnabled(request, httpServletRequest, httpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        // Copy fault tolerance testing settings from session to thread local storage
        if (ConnectionsRegistry.INSTANCE.isThreadLocal()) {
            copyFaultToleranceTestingFromSession((HttpServletRequest) request);
        }

        // process Sniffy REST calls
        if (null != sniffyServlet) {
            try {
                sniffyServlet.service(request, response);
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

        try {
            sniffyRequestProcessor.process(chain);
        } finally {

            // Clean fault tolerance testing settings thread local storage
            cleanThreadLocalFaultToleranceSettings();

        }

    }

    private static void cleanThreadLocalFaultToleranceSettings() {
        if (ConnectionsRegistry.INSTANCE.isThreadLocal()) {
            ConnectionsRegistry.INSTANCE.setThreadLocalDiscoveredAddresses(
                    new ConcurrentHashMap<Map.Entry<String, Integer>, ConnectionsRegistry.ConnectionStatus>()
            );
            ConnectionsRegistry.INSTANCE.setThreadLocalDiscoveredDataSources(
                    new ConcurrentHashMap<Map.Entry<String, String>, ConnectionsRegistry.ConnectionStatus>()
            );
        }
    }

    private static void copyFaultToleranceTestingFromSession(HttpServletRequest request) {

        HttpSession session = request.getSession();

        Map<Map.Entry<String,Integer>, ConnectionsRegistry.ConnectionStatus> discoveredAddresses =
                (Map<Map.Entry<String,Integer>, ConnectionsRegistry.ConnectionStatus>)
                        session.getAttribute(THREAD_LOCAL_DISCOVERED_ADDRESSES);
        if (null == discoveredAddresses) {
            discoveredAddresses = new ConcurrentHashMap<Map.Entry<String,Integer>, ConnectionsRegistry.ConnectionStatus>();
            session.setAttribute(THREAD_LOCAL_DISCOVERED_ADDRESSES, discoveredAddresses);
        }
        ConnectionsRegistry.INSTANCE.setThreadLocalDiscoveredAddresses(discoveredAddresses);

        Map<Map.Entry<String,String>, ConnectionsRegistry.ConnectionStatus> discoveredDataSources =
                (Map<Map.Entry<String,String>, ConnectionsRegistry.ConnectionStatus>)
                        session.getAttribute(THREAD_LOCAL_DISCOVERED_DATA_SOURCES);
        if (null == discoveredDataSources) {
            discoveredDataSources = new ConcurrentHashMap<Map.Entry<String,String>, ConnectionsRegistry.ConnectionStatus>();
            session.setAttribute(THREAD_LOCAL_DISCOVERED_DATA_SOURCES, discoveredDataSources);
        }

        ConnectionsRegistry.INSTANCE.setThreadLocalDiscoveredDataSources(discoveredDataSources);

    }

    private boolean isSniffyFilterEnabled(ServletRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws MalformedURLException {

        boolean sniffyEnabled = enabled;

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

        return sniffyEnabled;

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
        // TODO: implement some cleanup here if required
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
