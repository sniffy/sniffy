package com.github.bedrin.jdbc.sniffer.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

class BufferedServletResponseWrapper extends HttpServletResponseWrapper {

    private BufferedServletOutputStream bufferedServletOutputStream;

    private ServletOutputStream outputStream;
    private PrintWriter writer;

    public BufferedServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    protected BufferedServletOutputStream getBufferedServletOutputStream() throws IOException {
        if (null == bufferedServletOutputStream) {
            bufferedServletOutputStream = new BufferedServletOutputStream(super.getOutputStream());
        }
        return bufferedServletOutputStream;
    }

    public void doFlushAndClose() throws IOException {
        bufferedServletOutputStream.doFlushAndClose();
    }

    // headers relates methods

    @Override
    public void sendError(int sc, String msg) throws IOException {
        bufferedServletOutputStream.checkNotFlushed();
        super.sendError(sc, msg);
    }

    // content related methods

    @Override
    public void setBufferSize(int size) {
        if (null != bufferedServletOutputStream) bufferedServletOutputStream.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
        return null == bufferedServletOutputStream ? super.getBufferSize() : bufferedServletOutputStream.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        getBufferedServletOutputStream().flush();
    }

    @Override
    public void resetBuffer() {
        if (null != bufferedServletOutputStream) bufferedServletOutputStream.reset();
    }

    @Override
    public boolean isCommitted() {
        return (null != bufferedServletOutputStream && bufferedServletOutputStream.isFlushed());
    }

    @Override
    public void reset() {
        resetBuffer();
        super.reset();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (null != outputStream) {
            return outputStream;
        } else if (null != writer) {
            throw new IllegalStateException("getWriter() method has been called on this response");
        } else {
            return outputStream = bufferedServletOutputStream =
                    new BufferedServletOutputStream(super.getOutputStream());
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (null != writer) {
            return writer;
        } else if (null != outputStream) {
            throw new IllegalStateException("getOutputStream() method has been called on this response");
        } else {
            return writer = new PrintWriter(
                    bufferedServletOutputStream = new BufferedServletOutputStream(super.getOutputStream())
            );
        }
    }

}
