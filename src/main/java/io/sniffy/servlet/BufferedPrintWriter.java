package io.sniffy.servlet;

import java.io.PrintWriter;

public class BufferedPrintWriter extends PrintWriter {

    private final BufferedServletOutputStream bufferedServletOutputStream;

    public BufferedPrintWriter(BufferedServletOutputStream bufferedServletOutputStream) {
        super(bufferedServletOutputStream, false);
        this.bufferedServletOutputStream = bufferedServletOutputStream;
    }
}
