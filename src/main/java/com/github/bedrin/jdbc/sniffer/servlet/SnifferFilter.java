package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.*;
import com.github.bedrin.jdbc.sniffer.sql.StatementMetaData;
import com.github.bedrin.jdbc.sniffer.util.LruCache;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.*;
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
 *        <filter-class>com.github.bedrin.jdbc.sniffer.servlet.SnifferFilter</filter-class>
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

    public final static String HEADER_NAME = "X-Sql-Queries";

    public static final String SNIFFER_URI_PREFIX =
            "/jdbcsniffer/" +
                    Constants.MAJOR_VERSION +
                    "." +
                    Constants.MINOR_VERSION +
                    "." +
                    Constants.PATCH_VERSION;

    public static final String JAVASCRIPT_URI = SNIFFER_URI_PREFIX + "/jdbcsniffer.min.js";
    public static final String CSS_URI = SNIFFER_URI_PREFIX + "/jdbcsniffer.css";
    public static final String REQUEST_URI_PREFIX = SNIFFER_URI_PREFIX + "/request/";

    private SnifferServlet snifferServlet;

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
        snifferServlet.init(new FilterServletConfigAdapter(filterConfig, "jdbc-sniffer"));

    }

    protected boolean injectHtml = false;
    protected boolean enabled = true;
    protected Pattern excludePattern = null;

    // TODO: consider replacing with some concurrent collection instead
    protected Map<String, List<StatementMetaData>> cache = Collections.synchronizedMap(
            new LruCache<String, List<StatementMetaData>>(10000)
    );

    public void doFilter(final ServletRequest request, ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        final String contextPath = httpServletRequest.getContextPath();
        final String relativeUrl = httpServletRequest.getRequestURI().substring(contextPath.length());

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        if (injectHtml) {
            snifferServlet.service(request, response);
            if (response.isCommitted()) return;
        }

        if (null != excludePattern && excludePattern.matcher(relativeUrl).matches()) {
            chain.doFilter(request, response);
            return;
        }

        BufferedServletResponseWrapper responseWrapper = null;

        final Spy<? extends Spy> spy = Sniffer.spy();
        final String requestId = UUID.randomUUID().toString();

        try {
            response = responseWrapper = new BufferedServletResponseWrapper(
                    httpServletResponse,
                    new BufferedServletResponseListener() {

                        /**
                         * Flag indicating that current response looks like HTML and capable of injecting sniffer widget
                         */
                        private boolean isHtmlPage = false;

                        /**
                         * todo return flag indicating that sniffer wont modify the output stream
                         * @param wrapper
                         * @param buffer
                         * @throws IOException
                         */
                        @Override
                        public void onBeforeCommit(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {
                            wrapper.addIntHeader(HEADER_NAME, spy.executedStatements(Threads.CURRENT));
                            wrapper.addHeader("X-Request-Id", requestId);
                            if (injectHtml) {
                                String contentType = wrapper.getContentType();
                                String contentEncoding = wrapper.getContentEncoding();

                                String mimeTypeMagic =
                                        URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(buffer.leadingBytes(16)));

                                if (null == contentEncoding && null != contentType && contentType.startsWith("text/html")
                                        && !"application/xml".equals(mimeTypeMagic)) {
                                    // adjust content length with the size of injected content
                                    int contentLength = wrapper.getContentLength();
                                    if (contentLength > 0) {
                                        wrapper.setContentLength(contentLength + maximumInjectSize(contextPath));
                                    }
                                    isHtmlPage = true;
                                }
                            }
                        }

                        @Override
                        public void beforeClose(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {

                            cache.put(requestId, spy.getExecutedStatements(Threads.CURRENT));

                            if (injectHtml && isHtmlPage) {

                                String characterEncoding = wrapper.getCharacterEncoding();
                                if (null == characterEncoding) {
                                    characterEncoding = Charset.defaultCharset().name();
                                }

                                String snifferWidget = generateAndPadHtml(contextPath, spy.executedStatements(Threads.CURRENT), requestId);

                                HtmlInjector htmlInjector = new HtmlInjector(buffer, characterEncoding);
                                htmlInjector.injectAtTheEnd(snifferWidget);

                            }

                        }

                    }
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        chain.doFilter(request, response);

        cache.put(requestId, spy.getExecutedStatements(Threads.CURRENT));

        if (null != responseWrapper) {
            responseWrapper.close();
        }

    }

    private static int MAXIMUM_INJECT_SIZE;

    protected static int maximumInjectSize(String contextPath) {
        if (MAXIMUM_INJECT_SIZE == 0) {
            MAXIMUM_INJECT_SIZE = generateHtml(contextPath, Integer.MAX_VALUE, UUID.randomUUID().toString()).length();
        }
        return MAXIMUM_INJECT_SIZE;
    }

    protected static String generateAndPadHtml(String contextPath, int executedQueries, String requestId) {
        StringBuilder sb = generateHtml(contextPath, executedQueries, requestId);
        for (int i = sb.length(); i < maximumInjectSize(contextPath); i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Generates following HTML snippet
     * <pre>
     * {@code
     * <div style="display:none!important" id="jdbc-sniffer" data-sql-queries="5" data-request-id="abcd"></div>
     * <script type="application-javascript" src="/petstore/jdbcsniffer.min.js"></script>
     * }
     * </pre>
     * @param executedQueries
     * @param requestId
     * @return
     */
    protected static StringBuilder generateHtml(String contextPath, int executedQueries, String requestId) {
        return new StringBuilder().
                append("<script id=\"jdbc-sniffer\" type=\"application/javascript\" data-sql-queries=\"").
                append(executedQueries).
                append("\" data-request-id=\"").
                append(requestId).
                append("\" src=\"").
                append(contextPath).
                append(JAVASCRIPT_URI).
                append("\"></script>");
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
