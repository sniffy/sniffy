package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.Sniffer;
import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.Threads;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP Filter will capture the number of executed queries for given HTTP request and return it
 * as a 'X-Sql-Queries' header in response.
 * @since 2.3.0
 */
public class SnifferFilter implements Filter {

    public final static String HEADER_NAME = "X-Sql-Queries";

    public void init(FilterConfig filterConfig) throws ServletException {
        String injectHtml = filterConfig.getInitParameter("inject-html");
        if (null != injectHtml) {
            this.injectHtml = Boolean.parseBoolean(injectHtml);
        }
    }

    private boolean injectHtml = false;

    public final static String HTML_START =
            "<div id=\"jdbc-sniffer-icon\" style=\"color:red;text-align:center;font-weight:bold;position:fixed;right:20px;bottom:20px;width:24px;height:24px;background-size:100%;z-index:9999999990;opacity:0.2;" +
                    "background-image:url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAADAklEQVRIibWWzWscdRzGP59hCEGWEEIstZYQQntICbnqxYuI4E0EPcQ/QQ9eJd7s4lEEUfCil+5JL8WLJ98u6qmEkMUXqgQsNYRYllJKCPN4mNmd2aTZouBv2d1nZn6/5/v6fHcFGOxdB10GNoCNwFXCKuSiupRkAZxTCpICrCLHkJFwGDgQ94FfgT1gl3Bn69o2Dvb6a8i7wAvAcqCQdgXw3+FKOAK+S/J2ibwBbI0JTx/yFB5fzMAFYRl5Rb1dAkXX3RBE7IQxHVFqsm6I52BhvpyKT3CK7uxy4vLZ3DwKFy3fKc/OWWk2pXnNxAkFUJFuMrvRnv1sTraXM7BIAXyPHpzNXxvnNBYUdTbGO8DXAtwYXr8sPg88B2wCVwJLj0rvjHUI/ELYQb4Fvtla377rYNhfS3L4+rV3Rt3dg2G/l3DRWhs9pSR1zRIq5RgYEY6Qg6317fvd8zeG/UVh2cGw/wHwMuGHmB/F3YQ/lFGSh8ix8QSppitkAZTAXMK8spiwhmwIz0CeBT8vSUBXkBXxNQBrspFwL/AAeUA4QaqQohZfSuITSk+zkLCgtppCEooSrcZOpS1oEVgUF22aoyOVutQJ2FQntMKcbK7FWkITuh3LnVym86yma1xoGGdi6jExKU7bKZnyeMr7tN+PxTAqgfeAv0heRTeSLE+8bFg7UbdpoXtzgquQA3UX+CLJZyVwAnwEfpKwhF4CrggrwFPAEtID5gllasGfBB4KI+Ao+CdhX/lNvdu0bqWWJeRN8GnkK+En4NbW+vYt/sMa7PVBFjEvgi8BvzsY9t8H3mpGdYXuQ24b9pFDwt/gCDkmnDRcZcic2kvyJHihbvOsES7VI0iAD8uJ+TqHhbAaXG1SQWwfT+7RGeu2Az6d35E05eoI43/oImsDx2OTCfWYjQ1uzHVw3UVjS4/DVAXwKeRL5F7trnVamvdEaGM8FqKeg6kCRyQ3gY8FGAz7BeEysAnZRK9St+kFYAno0aSzHl0h9QC8b012AOyjPwM7wM74b8s/Owp6BPc7ajsAAAAASUVORK5CYII=);\">";

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        BufferedServletResponseWrapper responseWrapper = null;

        try {
            final Spy spy = Sniffer.spy();
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
                            // inject html at the very end of output stream
                            wrapper.addCloseResponseListener(new CloseResponseListener() {
                                @Override
                                public void beforeClose(HttpServletResponse response, BufferedServletResponseWrapper wrapper) throws IOException {
                                    BufferedServletOutputStream bufferedServletOutputStream = wrapper.getBufferedServletOutputStream();
                                    bufferedServletOutputStream.write(
                                            generateAndPadHtml(spy.executedStatements(Threads.CURRENT)).getBytes()
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

    }

    private static int MAXIMUM_INJECT_SIZE;

    protected static int maximumInjectSize() {
        if (MAXIMUM_INJECT_SIZE == 0) {
            MAXIMUM_INJECT_SIZE = generateHtml(Integer.MAX_VALUE).length();
        }
        return MAXIMUM_INJECT_SIZE;
    }

    protected static String generateAndPadHtml(int executedQueries) {
        StringBuilder sb = generateHtml(executedQueries);
        for (int i = sb.length(); i < maximumInjectSize(); i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    protected static StringBuilder generateHtml(int executedQueries) {
        return new StringBuilder(HTML_START).
                append(executedQueries).
                append("</div>").
                append("<script type=\"application/javascript\">window.document.sqlQueries=").
                append(executedQueries).
                append(";</script>");
    }

    public void destroy() {

    }

}
