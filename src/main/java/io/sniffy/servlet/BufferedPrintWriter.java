package io.sniffy.servlet;

import java.io.IOException;
import java.io.PrintWriter;

public class BufferedPrintWriter extends PrintWriter {

    public BufferedPrintWriter(BufferedServletOutputStream bufferedServletOutputStream) {
        super(bufferedServletOutputStream, false);
    }

    public void flushIfOpen() throws IOException {
        if (null != out) {
            flush();
        }
    }

}