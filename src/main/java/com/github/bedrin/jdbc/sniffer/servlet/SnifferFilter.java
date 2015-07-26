package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.Sniffer;
import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.Threads;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SnifferFilter implements Filter {

    public final static String HEADER_NAME = "X-JDBC-SNIFFER-NUMBER-QUERIES";

    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        Spy spy = null;

        try {
            try {
                spy = Sniffer.spy();
            } catch (Exception e) {
                e.printStackTrace();
            }
            chain.doFilter(request, response);
        } finally {
            if (null != spy) try {
                HttpServletResponse httpServletResponse = HttpServletResponse.class.cast(response);
                httpServletResponse.addIntHeader(HEADER_NAME, spy.executedStatements(Threads.CURRENT));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void destroy() {

    }

}
