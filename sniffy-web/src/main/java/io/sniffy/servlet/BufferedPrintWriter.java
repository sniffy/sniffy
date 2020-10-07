package io.sniffy.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

class BufferedPrintWriter extends PrintWriter {

    private final BufferedServletOutputStream bufferedServletOutputStream;

    BufferedPrintWriter(BufferedServletOutputStream bufferedServletOutputStream, String characterEncoding) throws UnsupportedEncodingException {
        super(null == characterEncoding ? new OutputStreamWriter(bufferedServletOutputStream) :
                new OutputStreamWriter(bufferedServletOutputStream, characterEncoding));
        this.bufferedServletOutputStream = bufferedServletOutputStream;
    }

    void flushIfOpen() throws IOException {
        if (null != out) {
            flush();
        }
    }

    @Override
    public void close() {
        try {
            bufferedServletOutputStream.setExplicitFlushDisabled(true);
            super.close();
        } finally {
            bufferedServletOutputStream.setExplicitFlushDisabled(false);
        }
    }
}