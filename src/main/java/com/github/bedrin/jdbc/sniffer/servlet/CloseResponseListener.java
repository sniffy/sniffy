package com.github.bedrin.jdbc.sniffer.servlet;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EventListener;

interface CloseResponseListener extends EventListener {

    void beforeClose(HttpServletResponse response, BufferedServletResponseWrapper wrapper) throws IOException;

}
