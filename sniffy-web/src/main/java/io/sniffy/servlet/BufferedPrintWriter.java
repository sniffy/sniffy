package io.sniffy.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

class BufferedPrintWriter extends PrintWriter {

    BufferedPrintWriter(BufferedServletOutputStream bufferedServletOutputStream, String characterEncoding) throws UnsupportedEncodingException {
        super(null == characterEncoding ? new OutputStreamWriter(bufferedServletOutputStream) :
                new OutputStreamWriter(bufferedServletOutputStream, characterEncoding));
    }

    void flushIfOpen() throws IOException {
        if (null != out) {
            flush();
        }
    }

}