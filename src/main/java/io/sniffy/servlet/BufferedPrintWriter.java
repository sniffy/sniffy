package io.sniffy.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class BufferedPrintWriter extends PrintWriter {

    public BufferedPrintWriter(BufferedServletOutputStream bufferedServletOutputStream) {
        super(new OutputStreamWriter(bufferedServletOutputStream));
    }

    public void flushIfOpen() throws IOException {
        if (null != out) {
            flush();
        }
    }

}