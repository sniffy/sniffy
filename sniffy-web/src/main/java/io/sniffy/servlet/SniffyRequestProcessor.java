package io.sniffy.servlet;

import io.sniffy.CurrentThreadSpy;
import io.sniffy.Sniffy;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

import static io.sniffy.servlet.SniffyFilter.*;

class SniffyRequestProcessor implements BufferedServletResponseListener {

    private final SniffyFilter sniffyFilter;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;

    private final CurrentThreadSpy spy;
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

    public SniffyRequestProcessor(SniffyFilter sniffyFilter, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        this.sniffyFilter = sniffyFilter;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;

        spy = Sniffy.spyCurrentThread();
        requestId = UUID.randomUUID().toString();

        String relativeUrl = null;

        try {
            String contextPath = httpServletRequest.getContextPath(); // like "/petclinic"
            relativeUrl = null == httpServletRequest.getRequestURI() ? null : httpServletRequest.getRequestURI().substring(contextPath.length());
        } catch (Exception e) {
            if (null != sniffyFilter.servletContext) {
                sniffyFilter.servletContext.log("Exception in SniffyRequestProcessor; calling original chain", e);
            } else {
                e.printStackTrace();
            }
        }

        this.relativeUrl = relativeUrl;
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
        } finally {
            try {
                requestStats.setTimeToFirstByte(getTimeToFirstByte());
                requestStats.setElapsedTime(getElapsedTime());
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
                (null != socketOperations && !socketOperations.isEmpty())) {
            if (null != executedStatements && !executedStatements.isEmpty()) {
                requestStats.setExecutedStatements(executedStatements);
            }
            if (null != socketOperations && !socketOperations.isEmpty()) {
                requestStats.setSocketOperations(socketOperations);
            }
            sniffyFilter.cache.put(requestId, requestStats);
        }
    }

    /**
     * Flag indicating that current response looks like HTML and capable of injecting sniffer widget
     */
    private boolean isHtmlPage = false;

    @Override
    public void onBeforeCommit(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {
        wrapper.addCorsHeadersHeaderIfRequired();
        wrapper.addIntHeader(HEADER_NUMBER_OF_QUERIES, spy.executedStatements());
        wrapper.addHeader(HEADER_TIME_TO_FIRST_BYTE, Long.toString(getTimeToFirstByte()));

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

        wrapper.addHeader(HEADER_REQUEST_DETAILS, sb.toString());

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
