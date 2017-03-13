package io.sniffy.servlet;

import io.sniffy.CurrentThreadSpy;
import io.sniffy.Sniffy;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.ExceptionUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

import static io.sniffy.servlet.SniffyFilter.*;

/**
 * @see SniffyFilter
 * @since 2.3.0
 */
class SniffyRequestProcessor implements BufferedServletResponseListener {

    private final static String SNIFFY_REQUEST_STATS_REQUEST_ATTRIBUTE_NAME =
            "io.sniffy.servlet.RequestStats";
    public final static String SNIFFY_REQUEST_PROCESSOR_REQUEST_ATTRIBUTE_NAME =
                    "io.sniffy.servlet.SniffyRequestProcessor";
    public final static String SNIFFY_REQUEST_ID_REQUEST_ATTRIBUTE_NAME =
                    "io.sniffy.servlet.SniffyRequestProcessor.requestId";

    private final SniffyFilter sniffyFilter;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;

    private final CurrentThreadSpy spy;
    private final String requestId;
    private final RequestStats requestStats;
    private final String relativeUrl;

    private long startMillis;
    private long timeToFirstByte;
    private long elapsedTime;

    public void initStartMillis() {
        startMillis = System.currentTimeMillis();
    }

    public long getTimeToFirstByte() {
        if (0 == timeToFirstByte) timeToFirstByte = System.currentTimeMillis() - startMillis;
        return timeToFirstByte;
    }

    public long getElapsedTime() {
        if (0 == elapsedTime) elapsedTime = System.currentTimeMillis() - startMillis;
        return elapsedTime;
    }

    public SniffyRequestProcessor(SniffyFilter sniffyFilter, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        this.sniffyFilter = sniffyFilter;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;

        spy = Sniffy.spyCurrentThread();

        Object requestIdAttribute = httpServletRequest.getAttribute(SNIFFY_REQUEST_ID_REQUEST_ATTRIBUTE_NAME);
        if (requestIdAttribute instanceof String) {
            requestId = (String) requestIdAttribute;
        } else {
            requestId = UUID.randomUUID().toString();
            httpServletRequest.setAttribute(SNIFFY_REQUEST_ID_REQUEST_ATTRIBUTE_NAME, requestId);
        }

        Object requestStatsAttribute = httpServletRequest.getAttribute(SNIFFY_REQUEST_STATS_REQUEST_ATTRIBUTE_NAME);
        if (requestStatsAttribute instanceof RequestStats) {
            requestStats = (RequestStats) requestStatsAttribute;
        } else {
            requestStats = new RequestStats();
            httpServletRequest.setAttribute(SNIFFY_REQUEST_STATS_REQUEST_ATTRIBUTE_NAME, requestStats);
        }

        String relativeUrl = null;

        try {
            relativeUrl = getBestContextURI(httpServletRequest);
        } catch (Exception e) {
            if (null != sniffyFilter.servletContext) {
                sniffyFilter.servletContext.log("Exception in SniffyRequestProcessor; calling original chain", e);
            } else {
                e.printStackTrace();
            }
        }

        this.relativeUrl = relativeUrl;
    }

    public static String getBestContextURI(HttpServletRequest httpServletRequest) {

        String requestURI = httpServletRequest.getRequestURI();
        if (null == requestURI) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(httpServletRequest.getContextPath()); // like "/petclinic"

        String servletPath = httpServletRequest.getServletPath();
        if (null != servletPath) {

            String pathInfo = httpServletRequest.getPathInfo();

            if (null != pathInfo && !pathInfo.isEmpty()) {
                sb.append(servletPath); // like "/petclinic/servlet"
            } else {
                // TODO: check if it is path mapping or exact mapping
            }

        }

        return requestURI.substring(sb.length());
    }

    public void process(FilterChain chain) throws IOException, ServletException {
        try {
            processImpl(chain);
        } finally {
            spy.close();
        }
    }

    private void processImpl(FilterChain chain) throws IOException, ServletException {
        // if excluded by pattern return immediately

        if (null != sniffyFilter.excludePattern && null != relativeUrl && sniffyFilter.excludePattern.matcher(relativeUrl).matches()) {
            chain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        // create response wrapper

        BufferedServletResponseWrapper responseWrapper;

        try {
            responseWrapper = new BufferedServletResponseWrapper(httpServletResponse, this);
        } catch (Exception e) {
            if (null != sniffyFilter.servletContext) {
                sniffyFilter.servletContext.log("Exception in SniffyRequestProcessor; calling original chain", e);
            } else {
                e.printStackTrace();
            }
            chain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        // call chain
        try {
            initStartMillis();
            chain.doFilter(httpServletRequest, responseWrapper);
        } catch (Throwable t) {
            requestStats.addException(t);
            ExceptionUtil.throwException(t);
        } finally {
            try {
                requestStats.incTimeToFirstByte(getTimeToFirstByte());
                requestStats.incElapsedTime(getElapsedTime());
                updateRequestCache();
                responseWrapper.flushIfPossible();
            } catch (Exception e) {
                if (null != sniffyFilter.servletContext) {
                    sniffyFilter.servletContext.log("Exception in SniffyRequestProcessor; original chain was already called", e);
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateRequestCache() {
        Map<StatementMetaData, SqlStats> executedStatements = spy.getExecutedStatements();
        Map<SocketMetaData, SocketStats> socketOperations = spy.getSocketOperations();
        if ((null != executedStatements && !executedStatements.isEmpty()) ||
                (null != socketOperations && !socketOperations.isEmpty()) ||
                (null != requestStats.getExceptions() && !requestStats.getExceptions().isEmpty())) {
            if (null != executedStatements && !executedStatements.isEmpty()) {
                requestStats.addExecutedStatements(executedStatements);
            }
            if (null != socketOperations && !socketOperations.isEmpty()) {
                requestStats.addSocketOperations(socketOperations);
            }
            sniffyFilter.cache.put(requestId, requestStats);
        }
        httpServletRequest.setAttribute(SNIFFY_REQUEST_STATS_REQUEST_ATTRIBUTE_NAME, requestStats);
    }

    /**
     * Flag indicating that current response looks like HTML and capable of injecting sniffer widget
     */
    private boolean isHtmlPage = false;

    @Override
    public void onBeforeCommit(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {
        wrapper.addCorsHeadersHeaderIfRequired();
        wrapper.setIntHeader(HEADER_NUMBER_OF_QUERIES, requestStats.executedStatements() + spy.executedStatements());
        wrapper.setHeader(HEADER_TIME_TO_FIRST_BYTE, requestStats.getElapsedTime() + Long.toString(getTimeToFirstByte()));
        // TODO: store startTime of first request processor somewhere

        StringBuilder sb = new StringBuilder();
        String contextRelativePath;

        if (null == relativeUrl || relativeUrl.isEmpty()) {
            contextRelativePath = "./";
            sb.append(contextRelativePath);
        } else {
            for (int i = 1; i < relativeUrl.length(); i++) {
                if ('/' == relativeUrl.charAt(i)) {
                    sb.append("../");
                }
            }

            contextRelativePath = sb.toString();
        }

        sb.append(REQUEST_URI_PREFIX).append(requestId);

        wrapper.setHeader(HEADER_REQUEST_DETAILS, sb.toString());

        if (sniffyFilter.injectHtml) {
            String contentType = wrapper.getContentType();
            String characterEncoding = wrapper.getCharacterEncoding();

            if (null != buffer && null != contentType && contentType.startsWith("text/html")) {
                // adjust content length with the size of injected content
                long contentLength = wrapper.getContentLength();
                if (contentLength > 0) {
                    if (contentLength > Integer.MAX_VALUE) {
                        wrapper.setContentLengthLong(contentLength + maximumInjectSize(contextRelativePath));
                    } else {
                        wrapper.setContentLength((int) contentLength + maximumInjectSize(contextRelativePath));
                    }
                }
                isHtmlPage = true;

                if (null == characterEncoding) {
                    characterEncoding = Charset.defaultCharset().name();
                }

                String snifferHeader = generateHeaderHtml(contextRelativePath, requestId).toString();

                HtmlInjector htmlInjector = new HtmlInjector(buffer, characterEncoding);
                htmlInjector.injectAtTheBeginning(snifferHeader);

            }
        }
    }

    @Override
    public void beforeClose(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {

        updateRequestCache();

        if (sniffyFilter.injectHtml && isHtmlPage) {

            String characterEncoding = wrapper.getCharacterEncoding();
            if (null == characterEncoding) {
                characterEncoding = Charset.defaultCharset().name();
            }

            String snifferWidget = generateAndPadFooterHtml(spy.executedStatements(), getElapsedTime());

            HtmlInjector htmlInjector = new HtmlInjector(buffer, characterEncoding);
            htmlInjector.injectAtTheEnd(snifferWidget);

        }

    }

    protected StringBuilder generateHeaderHtml(String contextPath, String requestId) {
        if (contextPath.startsWith("./")) {
            return new StringBuilder().
                    append("<script type=\"application/javascript\">").
                    append("document.write('\\x3Cscript ").
                    append("id=\"sniffy-header\" type=\"application/javascript\" data-request-id=\"").
                    append(requestId).
                    append("\" src=\"'+location.href+'").
                    append(contextPath.substring(1)).
                    append(JAVASCRIPT_URI).
                    append("\">\\x3C/script>');</script>");
        } else {
            return new StringBuilder().
                    append("<script id=\"sniffy-header\" type=\"application/javascript\" data-request-id=\"").
                    append(requestId).
                    append("\" src=\"").
                    append(contextPath).
                    append(JAVASCRIPT_URI).
                    append("\"></script>");
        }
    }

    private int maximumInjectSize;

    protected int maximumInjectSize(String contextPath) {
        if (maximumInjectSize == 0) {
            maximumInjectSize = maximumFooterSize() +
                    generateHeaderHtml(contextPath, UUID.randomUUID().toString()).length();
        }
        return maximumInjectSize;
    }

    private int maximumFooterSize() {
        return generateFooterHtml(Integer.MAX_VALUE, Long.MAX_VALUE).length();
    }

    protected String generateAndPadFooterHtml(int executedQueries, long serverTime) {
        StringBuilder sb = generateFooterHtml(executedQueries, serverTime);
        for (int i = sb.length(); i < maximumFooterSize(); i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Generates following HTML snippet
     * <pre>
     * {@code
     * <data id="sniffy" data-sql-queries="5"/>
     * }
     * </pre>
     * @param executedQueries number of executed queries
     * @return StringBuilder with generated HTML
     */
    protected StringBuilder generateFooterHtml(int executedQueries, long serverTime) {
        return new StringBuilder().
                append("<data id=\"sniffy\" data-sql-queries=\"").
                append(executedQueries).
                append("\" data-server-time=\"").
                append(serverTime)
                .append("\"/>");
    }

}
