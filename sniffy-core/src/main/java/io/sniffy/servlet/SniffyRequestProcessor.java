package io.sniffy.servlet;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.Threads;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

import static io.sniffy.servlet.SnifferFilter.HEADER_NUMBER_OF_QUERIES;
import static io.sniffy.servlet.SnifferFilter.HEADER_REQUEST_DETAILS;
import static io.sniffy.servlet.SnifferFilter.HEADER_TIME_TO_FIRST_BYTE;

class SniffyRequestProcessor implements BufferedServletResponseListener {

    private final SnifferFilter snifferFilter;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;

    private final Spy<?> spy;
    private final String requestId;
    private final RequestStats requestStats = new RequestStats();
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

    public SniffyRequestProcessor(SnifferFilter snifferFilter, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        this.snifferFilter = snifferFilter;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;

        spy = Sniffy.spyCurrentThread();
        requestId = UUID.randomUUID().toString();

        String relativeUrl = null;

        try {
            String contextPath = httpServletRequest.getContextPath();
            relativeUrl = null == httpServletRequest.getRequestURI() ? null : httpServletRequest.getRequestURI().substring(contextPath.length());
        } catch (Exception e) {
            if (null != snifferFilter.servletContext) {
                snifferFilter.servletContext.log("Exception in SniffyRequestProcessor; calling original chain", e);
            } else {
                e.printStackTrace();
            }
        }

        this.relativeUrl = relativeUrl;
    }

    public void process(FilterChain chain) throws IOException, ServletException {

        // if excluded by pattern return immediately

        if (null != snifferFilter.excludePattern && null != relativeUrl && snifferFilter.excludePattern.matcher(relativeUrl).matches()) {
            chain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        // create response wrapper

        BufferedServletResponseWrapper responseWrapper;

        try {
            responseWrapper = new BufferedServletResponseWrapper(httpServletResponse, this);
        } catch (Exception e) {
            if (null != snifferFilter.servletContext) {
                snifferFilter.servletContext.log("Exception in SniffyRequestProcessor; calling original chain", e);
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
        } finally {
            try {
                requestStats.setTimeToFirstByte(getTimeToFirstByte());
                requestStats.setElapsedTime(getElapsedTime());
                updateRequestCache();
                responseWrapper.flushIfPossible();
            } catch (Exception e) {
                if (null != snifferFilter.servletContext) {
                    snifferFilter.servletContext.log("Exception in SniffyRequestProcessor; original chain was already called", e);
                } else {
                    e.printStackTrace();
                }
            }
        }

    }

    private void updateRequestCache() {
        Map<StatementMetaData, SqlStats> executedStatements = spy.getExecutedStatements(Threads.CURRENT, false);
        Map<SocketMetaData, SocketStats> socketOperations = spy.getSocketOperations(Threads.CURRENT, null, false);
        if ((null != executedStatements && !executedStatements.isEmpty()) ||
                (null != socketOperations && !socketOperations.isEmpty())) {
            if (null != executedStatements && !executedStatements.isEmpty()) {
                requestStats.setExecutedStatements(executedStatements);
            }
            if (null != socketOperations && !socketOperations.isEmpty()) {
                requestStats.setSocketOperations(socketOperations);
            }
            snifferFilter.cache.put(requestId, requestStats);
        }
    }

    /**
     * Flag indicating that current response looks like HTML and capable of injecting sniffer widget
     */
    private boolean isHtmlPage = false;

    @Override
    public void onBeforeCommit(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {
        wrapper.addCorsHeadersHeaderIfRequired();
        wrapper.addIntHeader(HEADER_NUMBER_OF_QUERIES, spy.executedStatements(Threads.CURRENT));
        wrapper.addHeader(HEADER_TIME_TO_FIRST_BYTE, Long.toString(getTimeToFirstByte()));

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < relativeUrl.length(); i++) {
            if ('/' == relativeUrl.charAt(i)) {
                if (sb.length() > 0) sb.append('/');
                sb.append("..");
            }
        }

        String contextRelativePath = sb.toString();

        sb.append(SnifferFilter.REQUEST_URI_PREFIX).append(requestId);

        wrapper.addHeader(HEADER_REQUEST_DETAILS, sb.toString());

        if (snifferFilter.injectHtml) {
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

        if (snifferFilter.injectHtml && isHtmlPage) {

            String characterEncoding = wrapper.getCharacterEncoding();
            if (null == characterEncoding) {
                characterEncoding = Charset.defaultCharset().name();
            }

            String snifferWidget = generateAndPadFooterHtml(spy.executedStatements(Threads.CURRENT), getElapsedTime());

            HtmlInjector htmlInjector = new HtmlInjector(buffer, characterEncoding);
            htmlInjector.injectAtTheEnd(snifferWidget);

        }

    }

    protected StringBuilder generateHeaderHtml(String contextPath, String requestId) {
        return new StringBuilder().
                append("<script id=\"sniffy-header\" type=\"application/javascript\" data-request-id=\"").
                append(requestId).
                append("\" src=\"").
                append(contextPath).
                append(SnifferFilter.JAVASCRIPT_URI).
                append("\"></script>");
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
