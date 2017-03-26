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

import static io.sniffy.servlet.SniffyRequestProcessor.SNIFFY_REQUEST_PROCESSOR_REQUEST_ATTRIBUTE_NAME;

/**
 * HTTP Filter will capture the number of executed queries for given HTTP request and return it
 * as a 'Sniffy-Sql-Queries' header in response.
 *
 * It also can inject an icon with a number of executed queries to each HTML page
 * This feature is can be enabled using inject-html filter parameter
 *
 * All feature are enabled by default
 *
 * Configuration in web.xml have higher precedence over system properties and environment variables
 *
 * Example of web.xml:
 * <pre>
 * {@code
 *   <filter>
 *        <filter-name>sniffer</filter-name>
 *        <filter-class>SniffyFilter</filter-class>
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
 * @since 3.1
 */
public class SniffyFilter implements Filter {

    public static final String HEADER_CORS_HEADERS = "Access-Control-Expose-Headers";
    public static final String HEADER_NUMBER_OF_QUERIES = "Sniffy-Sql-Queries";
    public static final String HEADER_TIME_TO_FIRST_BYTE = "Sniffy-Time-To-First-Byte";
    public static final String HEADER_REQUEST_DETAILS = "Sniffy-Request-Details";

    public static final String SNIFFY_URI_PREFIX =
            "sniffy/" +
                    Constants.MAJOR_VERSION +
                    "." +
                    Constants.MINOR_VERSION +
                    "." +
                    Constants.PATCH_VERSION;

    public static final String JAVASCRIPT_URI = SNIFFY_URI_PREFIX + "/sniffy.min.js";
    public static final String JAVASCRIPT_SOURCE_URI = SNIFFY_URI_PREFIX + "/sniffy.js";
    public static final String JAVASCRIPT_MAP_URI = SNIFFY_URI_PREFIX + "/sniffy.map";
    public static final String REQUEST_URI_PREFIX = SNIFFY_URI_PREFIX + "/request/";

    public static final String SNIFFY = "sniffy";

    public static final String SNIFFY_ENABLED_PARAMETER = "Sniffy-Enabled";
    public static final String INJECT_HTML_ENABLED_PARAMETER = "Sniffy-Inject-Html-Enabled";

    protected static final String THREAD_LOCAL_DISCOVERED_ADDRESSES = "discoveredAddresses";
    protected static final String THREAD_LOCAL_DISCOVERED_DATA_SOURCES = "discoveredDataSources";

    protected Boolean monitorSocket;

    protected boolean filterEnabled = true;
    protected Pattern excludePattern = null;

    protected boolean injectHtml = true;
    protected Pattern injectHtmlExcludePattern = null;

    protected final Map<String, RequestStats> cache = new ConcurrentLinkedHashMap.Builder<String, RequestStats>().
            maximumWeightedCapacity(200).
            build();

    protected SniffyServlet sniffyServlet = new SniffyServlet(cache);
    protected ServletContext servletContext; // TODO: log via slf4j if available

    public SniffyFilter() {

        Boolean filterEnabled = SniffyConfiguration.INSTANCE.getFilterEnabled();
        this.filterEnabled = null == filterEnabled ? true : filterEnabled;

        try {
            String excludePattern = SniffyConfiguration.INSTANCE.getExcludePattern();
            if (null != excludePattern) {
                this.excludePattern = Pattern.compile(excludePattern);
            }
        } catch (PatternSyntaxException e) {
            // TODO: log me maybe?
        }

        Boolean injectHtmlEnabled = SniffyConfiguration.INSTANCE.getInjectHtmlEnabled();
        this.injectHtml = null == injectHtmlEnabled ? true : injectHtmlEnabled;

        try {
            String injectHtmlExcludePattern = SniffyConfiguration.INSTANCE.getInjectHtmlExcludePattern();
            if (null != injectHtmlExcludePattern) {
                this.injectHtmlExcludePattern = Pattern.compile(injectHtmlExcludePattern);
            }
        } catch (PatternSyntaxException e) {
            // TODO: log me maybe?
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {

        try {

            // Proceed only if filter config is provided and neither setEnabled() nor setMonitorSocket() methods were called
            String monitorSocket = filterConfig.getInitParameter("monitor-socket");
            if (null == this.monitorSocket && (null == monitorSocket || Boolean.parseBoolean(monitorSocket))) {
                setMonitorSocket(true);
            }

            // TODO: rename to filter-enabled for consistency
            String filterEnabled = filterConfig.getInitParameter("enabled");
            if (null != filterEnabled) {
                if ("system".equals(filterEnabled)) {
                    this.filterEnabled = Boolean.TRUE.equals(SniffyConfiguration.INSTANCE.getFilterEnabled());
                } else {
                    this.filterEnabled = Boolean.parseBoolean(filterEnabled);
                }
            }

            String excludePattern = filterConfig.getInitParameter("exclude-pattern");
            if (null != excludePattern) {
                this.excludePattern = Pattern.compile(excludePattern); // TODO: can throw exception
            }

            String injectHtmlEnabled = filterConfig.getInitParameter("inject-html");
            if (null != injectHtmlEnabled) {
                if ("system".equals(injectHtmlEnabled)) {
                    this.injectHtml = Boolean.TRUE.equals(SniffyConfiguration.INSTANCE.getInjectHtmlEnabled());
                } else {
                    this.injectHtml = Boolean.parseBoolean(injectHtmlEnabled);
                }
            }

            String injectHtmlExcludePattern = filterConfig.getInitParameter("inject-html-exclude-pattern");
            if (null != injectHtmlExcludePattern) {
                this.injectHtmlExcludePattern = Pattern.compile(injectHtmlExcludePattern); // TODO: can throw exception
            }

            String faultToleranceCurrentRequest = filterConfig.getInitParameter("fault-tolerance-current-request");
            if (null != faultToleranceCurrentRequest) {
                ConnectionsRegistry.INSTANCE.setThreadLocal(Boolean.parseBoolean(faultToleranceCurrentRequest));
            }

            sniffyServlet.init(new FilterServletConfigAdapter(filterConfig, "sniffy"));

            servletContext = filterConfig.getServletContext();

        } catch (Exception e) {
            e.printStackTrace();
            filterEnabled = false;
        }

    }

    public void doFilter(final ServletRequest request, ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        // issues/275 - Sniffy filter is called twice in case of request forwarding
        Object existingRequestProcessorAttribute = request.getAttribute(SNIFFY_REQUEST_PROCESSOR_REQUEST_ATTRIBUTE_NAME);
        if (null != existingRequestProcessorAttribute) {
            chain.doFilter(request, response);
            return;
        }

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
                if (response.isCommitted()) return; // TODO: seems like it doesn't work sometimes
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
            request.setAttribute(SNIFFY_REQUEST_PROCESSOR_REQUEST_ATTRIBUTE_NAME, sniffyRequestProcessor);
        } catch (Exception e) {
            if (null != servletContext) servletContext.log("Exception in SniffyRequestProcessor initialization; calling original chain", e);
            chain.doFilter(request, response);
            return;
        }

        try {
            sniffyRequestProcessor.process(chain);
        } finally {

            request.removeAttribute(SNIFFY_REQUEST_PROCESSOR_REQUEST_ATTRIBUTE_NAME);

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

        boolean sniffyEnabled = filterEnabled;

        // override by request parameter/cookie value if provided

        String sniffyEnabledParam = getQueryParam(httpServletRequest, SNIFFY);

        if (null != sniffyEnabledParam) {
            sniffyEnabled = Boolean.parseBoolean(sniffyEnabledParam);
            setSessionCookie(httpServletResponse, SNIFFY, String.valueOf(sniffyEnabled));
        } else {
            sniffyEnabledParam = readCookie(httpServletRequest, SNIFFY);
            if (null != sniffyEnabledParam) {
                sniffyEnabled = Boolean.parseBoolean(sniffyEnabledParam);
            }
        }

        // override by request header

        String sniffyEnabledHeader = httpServletRequest.getHeader(SNIFFY_ENABLED_PARAMETER);

        if (null != sniffyEnabledHeader) {
            sniffyEnabled = Boolean.parseBoolean(sniffyEnabledHeader);
        }

        return sniffyEnabled;

    }

    private String getQueryParam(HttpServletRequest httpServletRequest, String name) {
        String queryString = httpServletRequest.getQueryString();
        if (null != queryString) {
            String[] queryParams = queryString.split("&");
            for (String queryParam : queryParams) {
                if (null != queryParam) {
                    String[] split = queryParam.split("=");
                    if (split.length >= 1 && name.equals(split[0])) {
                        return split.length >= 2 ? split[1] : "true";
                    }
                }
            }
        }
        return null;
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

    public Pattern getInjectHtmlExcludePattern() {
        return injectHtmlExcludePattern;
    }

    public void setInjectHtmlExcludePattern(Pattern injectHtmlExcludePattern) {
        this.injectHtmlExcludePattern = injectHtmlExcludePattern;
    }

    /**
     * @see #isMonitorSocket()
     * @see #isFilterEnabled()
     */
    @Deprecated
    public boolean isEnabled() {
        return filterEnabled;
    }

    /**
     * @see #setMonitorSocket(boolean)
     * @see #setFilterEnabled(boolean)
     */
    @Deprecated
    public void setEnabled(boolean enabled) {
        setFilterEnabled(enabled);
        setMonitorSocket(enabled);
    }

    /**
     * @since 3.1.3
     */
    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    /**
     * @since 3.1.3
     */
    public void setFilterEnabled(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    /**
     * @since 3.1.3
     */
    public boolean isMonitorSocket() {
        return SniffyConfiguration.INSTANCE.isMonitorSocket();
    }

    /**
     * @since 3.1.3
     */
    public void setMonitorSocket(boolean monitorSocket) {
        this.monitorSocket = monitorSocket;
        SniffyConfiguration.INSTANCE.setMonitorSocket(monitorSocket);
    }

    public Pattern getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(Pattern excludePattern) {
        this.excludePattern = excludePattern;
    }
}
