package com.github.bedrin.jdbc.sniffer.servlet;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EventListener;

interface FlushResponseListener extends EventListener {

    void beforeFlush(HttpServletResponse response, BufferedServletResponseWrapper wrapper) throws IOException;

}
