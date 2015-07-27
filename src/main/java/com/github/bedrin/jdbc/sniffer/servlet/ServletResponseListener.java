package com.github.bedrin.jdbc.sniffer.servlet;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EventListener;

public interface ServletResponseListener extends EventListener {

    void beforeFlush(HttpServletResponse response) throws IOException;

}
