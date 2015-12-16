package io.sniffy.servlet;

import io.sniffy.Spy;
import io.sniffy.Threads;
import io.sniffy.sql.StatementMetaData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

class SniffyServletResponseListener implements BufferedServletResponseListener {

    private final SnifferFilter snifferFilter;

    private final String contextPath; // todo: should be the same for the whole filter

    private final Spy<? extends Spy> spy;
    private final String requestId;

    public SniffyServletResponseListener(SnifferFilter snifferFilter, String contextPath, Spy<? extends Spy> spy, String requestId) {
        this.snifferFilter = snifferFilter;
        this.contextPath = contextPath;
        this.spy = spy;
        this.requestId = requestId;
    }

    /**
     * Flag indicating that current response looks like HTML and capable of injecting sniffer widget
     */
    private boolean isHtmlPage = false;

    @Override
    public void onBeforeCommit(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {
        wrapper.addIntHeader(SnifferFilter.HEADER_NUMBER_OF_QUERIES, spy.executedStatements(Threads.CURRENT));
        wrapper.addHeader(SnifferFilter.HEADER_REQUEST_DETAILS, contextPath + SnifferFilter.REQUEST_URI_PREFIX + requestId);
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
                    wrapper.setContentLength(contentLength + maximumInjectSize(contextPath));
                }
                isHtmlPage = true;

                String characterEncoding = wrapper.getCharacterEncoding();
                if (null == characterEncoding) {
                    characterEncoding = Charset.defaultCharset().name();
                }

                String snifferHeader = generateHeaderHtml(contextPath, requestId).toString();

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
