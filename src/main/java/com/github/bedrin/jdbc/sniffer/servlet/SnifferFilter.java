package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.Sniffer;
import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.Threads;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * HTTP Filter will capture the number of executed queries for given HTTP request and return it
 * as a 'X-JDBC-SNIFFER-NUMBER-QUERIES' header in response.
 * @since 2.3.0
 */
public class SnifferFilter implements Filter {

    public final static String HEADER_NAME = "X-Sql-Queries";

    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        Spy spy = null;
        BufferedServletResponseWrapper responseWrapper = null;

        try {
            try {
                spy = Sniffer.spy();
                response = responseWrapper = new BufferedServletResponseWrapper(HttpServletResponse.class.cast(response));
            } catch (Exception e) {
                e.printStackTrace();
            }
            chain.doFilter(request, response);
        } finally {
            if (null != spy && null != responseWrapper) try {
                responseWrapper.addIntHeader(HEADER_NAME, spy.executedStatements(Threads.CURRENT));
                responseWrapper.doFlushAndClose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void destroy() {

    }

}
