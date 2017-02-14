package io.sniffy.servlet;

import java.io.IOException;
import java.util.EventListener;

/**
 * @see SniffyFilter
 * @since 2.3.0
 */
interface BufferedServletResponseListener extends EventListener {

    void onBeforeCommit(BufferedServletResponseWrapper wrapper, Buffer buffer)
            throws IOException;

    void beforeClose(BufferedServletResponseWrapper wrapper, Buffer buffer)
            throws IOException;

}
