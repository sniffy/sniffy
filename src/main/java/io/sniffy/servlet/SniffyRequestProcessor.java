package io.sniffy.servlet;

import io.sniffy.Sniffer;
import io.sniffy.Spy;
import io.sniffy.Threads;
import io.sniffy.sql.StatementMetaData;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

class SniffyRequestProcessor implements BufferedServletResponseListener {

    private final SnifferFilter snifferFilter;
    private final ServletRequest request;
    private final ServletResponse response;

    private final Spy<?> spy;
    private final String requestId;

    public SniffyRequestProcessor(SnifferFilter snifferFilter, ServletRequest request, ServletResponse response) {
        this.snifferFilter = snifferFilter;
        this.request = request;
        this.response = response;

        spy = Sniffer.spy();
        requestId = UUID.randomUUID().toString();
    }

    public void process(FilterChain chain) throws IOException, ServletException {

        // extract some basic data from request and response

        HttpServletRequest httpServletRequest;
        HttpServletResponse httpServletResponse;
        String contextPath;
        String relativeUrl;

        try {

            httpServletRequest = (HttpServletRequest) request;
            httpServletResponse = (HttpServletResponse) response;
            contextPath = httpServletRequest.getContextPath();
            relativeUrl = null == httpServletRequest.getRequestURI() ? null :
                    httpServletRequest.getRequestURI().substring(contextPath.length());

        } catch (Exception e) {
            snifferFilter.servletContext.log("Exception in SniffyRequestProcessor; calling original chain", e);
            chain.doFilter(request, response);
            return;
        }

        // if excluded by pattern return immediately

        if (null != snifferFilter.excludePattern && null != relativeUrl && snifferFilter.excludePattern.matcher(relativeUrl).matches()) {
            chain.doFilter(request, response);
            return;
        }

        // create response wrapper

        BufferedServletResponseWrapper responseWrapper;

        try {
            responseWrapper = new BufferedServletResponseWrapper(httpServletResponse, this);
        } catch (Exception e) {
            snifferFilter.servletContext.log("Exception in SniffyRequestProcessor; calling original chain", e);
            chain.doFilter(request, response);
            return;
        }

        // call chain

        try {
            chain.doFilter(request, responseWrapper);
        } finally {

            try {

                // put details to the cache

                List<StatementMetaData> executedStatements = spy.getExecutedStatements(Threads.CURRENT);
                if (null != executedStatements && !executedStatements.isEmpty()) {
                    snifferFilter.cache.put(requestId, executedStatements);
                }

                // flush underlying stream if required

                responseWrapper.close();

            } catch (Exception e) {
                snifferFilter.servletContext.log("Exception in SniffyRequestProcessor; original chain was already called", e);
            }

        }

    }

    /**
     * Flag indicating that current response looks like HTML and capable of injecting sniffer widget
     */
    private boolean isHtmlPage = false;

    @Override
    public void onBeforeCommit(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {
        wrapper.addIntHeader(SnifferFilter.HEADER_NUMBER_OF_QUERIES, spy.executedStatements(Threads.CURRENT));
        wrapper.addHeader(SnifferFilter.HEADER_REQUEST_DETAILS, snifferFilter.contextPath + SnifferFilter.REQUEST_URI_PREFIX + requestId);
        if (snifferFilter.injectHtml) {
            String contentType = wrapper.getContentType();
            String contentEncoding = wrapper.getContentEncoding();

            String mimeTypeMagic = null == buffer ? null :
                    URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(buffer.leadingBytes(16)));

            if (null != buffer && null == contentEncoding && null != contentType && contentType.startsWith("text/html")
                    && !"application/xml".equals(mimeTypeMagic)) {
                // adjust content length with the size of injected content
                int contentLength = wrapper.getContentLength();
                if (contentLength > 0) {
                    wrapper.setContentLength(contentLength + maximumInjectSize(snifferFilter.contextPath));
                }
                isHtmlPage = true;

                String characterEncoding = wrapper.getCharacterEncoding();
                if (null == characterEncoding) {
                    characterEncoding = Charset.defaultCharset().name();
                }

                String snifferHeader = generateHeaderHtml(snifferFilter.contextPath, requestId).toString();

                HtmlInjector htmlInjector = new HtmlInjector(buffer, characterEncoding);
                htmlInjector.injectAtTheBeginning(snifferHeader);

            }
        }
    }

    @Override
    public void beforeClose(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {

        List<StatementMetaData> executedStatements = spy.getExecutedStatements(Threads.CURRENT);
        if (null != executedStatements && !executedStatements.isEmpty()) {
            snifferFilter.cache.put(requestId, executedStatements);
        }

        if (snifferFilter.injectHtml && isHtmlPage) {

            String characterEncoding = wrapper.getCharacterEncoding();
            if (null == characterEncoding) {
                characterEncoding = Charset.defaultCharset().name();
            }

            String snifferWidget = generateAndPadFooterHtml(spy.executedStatements(Threads.CURRENT));

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
        //return "<script type=\"application/javascript\" src=\"/mock/sniffy.min.js\"></script>";
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
        return generateFooterHtml(Integer.MAX_VALUE).length();
    }

    protected String generateAndPadFooterHtml(int executedQueries) {
        StringBuilder sb = generateFooterHtml(executedQueries);
        for (int i = sb.length(); i < maximumFooterSize(); i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Generates following HTML snippet
     * <pre>
     * {@code
     * <div style="display:none!important" id="sniffy" data-sql-queries="5" data-request-id="abcd"></div>
     * <script type="application-javascript" src="/petstore/sniffy.min.js"></script>
     * }
     * </pre>
     * @param executedQueries
     * @return
     */
    protected static StringBuilder generateFooterHtml(int executedQueries) {
        return new StringBuilder().
                append("<data id=\"sniffy\" data-sql-queries=\"").append(executedQueries).append("\"/>");
    }

}
