package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.Sniffer;
import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.Threads;
import com.github.bedrin.jdbc.sniffer.sql.StatementMetaData;
import com.github.bedrin.jdbc.sniffer.util.LruCache;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public static final String JAVASCRIPT_URI = "/monitor.js";

    public void init(FilterConfig filterConfig) throws ServletException {
        String injectHtml = filterConfig.getInitParameter("inject-html");
        if (null != injectHtml) {
            this.injectHtml = Boolean.parseBoolean(injectHtml);
        }
        String enabled = filterConfig.getInitParameter("enabled");
        if (null != enabled) {
            this.enabled = Boolean.parseBoolean(enabled);
        }
        contextPath = filterConfig.getServletContext().getContextPath();

        try {
            monitorJs = loadJavaScript();
        } catch (IOException e) {
            throw new ServletException(e);
        }

    }

    protected boolean injectHtml = false;

    protected boolean enabled = true;

    protected byte[] monitorJs;

    protected String contextPath;

    // TODO: consider replacing with some concurrent collection instead
    protected Map<String, List<StatementMetaData>> cache = Collections.synchronizedMap(
                new LruCache<String, List<StatementMetaData>>(10000)
    );

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        if (injectHtml) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getRequestURI().substring(contextPath.length());
            if (JAVASCRIPT_URI.equals(path)) {
                response.setContentType("application/javascript");
                response.setContentLength(monitorJs.length);

                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(monitorJs);
                outputStream.flush();

                return;
            }
        }

        BufferedServletResponseWrapper responseWrapper = null;

        final Spy spy = Sniffer.spy();
        final String requestId = UUID.randomUUID().toString();

        try {
            responseWrapper = new BufferedServletResponseWrapper((HttpServletResponse) response);

            responseWrapper.addFlushResponseListener(new FlushResponseListener() {
                @Override
                public void beforeFlush(HttpServletResponse response, BufferedServletResponseWrapper wrapper) throws IOException {
                    response.addIntHeader(HEADER_NAME, spy.executedStatements(Threads.CURRENT));
                    if (injectHtml) {
                        String contentType = response.getContentType();
                        String contentEncoding = wrapper.getContentEncoding();
                        if (null == contentEncoding && null != contentType && contentType.startsWith("text/html")) {
                            // adjust content length with the size of injected content
                            int contentLength = wrapper.getContentLength();
                            if (contentLength > 0) {
                                wrapper.setContentLength(contentLength + maximumInjectSize());
                            }
                            // add request id
                            response.addHeader("X-Request-Id", requestId);
                            // inject html at the very end of output stream
                            wrapper.addCloseResponseListener(new CloseResponseListener() {
                                @Override
                                public void beforeClose(HttpServletResponse response, BufferedServletResponseWrapper wrapper) throws IOException {
                                    BufferedServletOutputStream bufferedServletOutputStream = wrapper.getBufferedServletOutputStream();
                                    bufferedServletOutputStream.write(
                                            generateAndPadHtml(spy.executedStatements(Threads.CURRENT), requestId).getBytes()
                                    );
                                    bufferedServletOutputStream.flush();
                                }
                            });

                        }
                    }
                }
            });

            response = responseWrapper;

        } catch (Exception e) {
            e.printStackTrace();
        }

        chain.doFilter(request, response);

        if (null != responseWrapper) {
            responseWrapper.flush();
        }

        cache.put(requestId, spy.getExecutedStatements(Threads.CURRENT));

    }

    private static int MAXIMUM_INJECT_SIZE;

    protected int maximumInjectSize() {
        if (MAXIMUM_INJECT_SIZE == 0) {
            MAXIMUM_INJECT_SIZE = generateHtml(Integer.MAX_VALUE, UUID.randomUUID().toString()).length();
        }
        return MAXIMUM_INJECT_SIZE;
    }

    protected String generateAndPadHtml(int executedQueries, String requestId) {
        StringBuilder sb = generateHtml(executedQueries, requestId);
        for (int i = sb.length(); i < maximumInjectSize(); i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Generates following HTML snippet
     * <pre>
     * {@code
     * <div style="display:none!important" id="jdbc-sniffer" data-sql-queries="5" data-request-id="abcd"></div>
     * <script type="application-javascript" src=""></script>
     * }
     * </pre>
     * @param executedQueries
     * @param requestId
     * @return
     */
    protected StringBuilder generateHtml(int executedQueries, String requestId) {
        return new StringBuilder().
                append("<div style=\"display:none!important\" id=\"jdbc-sniffer\" data-sql-queries=\"").
                append(executedQueries).
                append("\" data-request-id=\"").
                append(requestId).
                append("\"></div>").
                append("<script type=\"application/javascript\" src=\"").
                append(contextPath + JAVASCRIPT_URI).
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

    protected static byte[] loadJavaScript() throws IOException {
        InputStream is = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is = SnifferFilter.class.getResourceAsStream("monitor.js");
            byte[] buff = new byte[1024];
            int count;
            while ((count = is.read(buff)) > 0) {
                baos.write(buff, 0, count);
            }
            return baos.toByteArray();
        } finally {
            if (null != is) {
                is.close();
            }
        }
    }

}
