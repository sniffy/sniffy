package io.sniffy.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

class BufferedServletResponseWrapper extends HttpServletResponseWrapper {

    private BufferedServletOutputStream bufferedServletOutputStream;
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    private boolean committed;

    private final BufferedServletResponseListener servletResponseListener;

    protected BufferedServletResponseWrapper(HttpServletResponse response,
                                             BufferedServletResponseListener servletResponseListener) {
        super(response);
        this.servletResponseListener = servletResponseListener;
    }

    protected void notifyBeforeCommit() throws IOException {
        notifyBeforeCommit(null);
    }

    protected void notifyBeforeCommit(Buffer buffer) throws IOException {
        servletResponseListener.onBeforeCommit(this, buffer);
    }

    protected void notifyBeforeClose() throws IOException {
        servletResponseListener.beforeClose(this, null);
    }

    protected void notifyBeforeClose(Buffer buffer) throws IOException {
        servletResponseListener.beforeClose(this, buffer);
    }

    protected BufferedServletOutputStream getBufferedServletOutputStream() throws IOException {
        if (null == bufferedServletOutputStream) {
            bufferedServletOutputStream = new BufferedServletOutputStream(this, super.getOutputStream());
        }
        return bufferedServletOutputStream;
    }

    /**
     * Flush the sniffer buffer and append the information about the executed queries to the output stream
     * Also close the underlying stream if it has been requested previously
     * @throws IOException
     */
    protected void close() throws IOException {
        if (null != writer) writer.close();
        else if (null != outputStream) outputStream.close();
        else {
            if (!isCommitted()) {
                notifyBeforeCommit();
            }
            notifyBeforeClose();
        }
    }

    protected void setCommitted(boolean committed) {
        this.committed = committed;
    }

    protected void setCommitted() {
        setCommitted(true);
    }

    // capture content length

    private int contentLength;

    @Override
    public void setContentLength(int len) {
        super.setContentLength(contentLength = len);
    }

    public int getContentLength() {
        return contentLength;
    }

    private String contentEncoding;

    @Override
    public void addHeader(String name, String value) {
        super.addHeader(name, value);
        if ("Content-Encoding".equals(name)) {
            contentEncoding = value;
        } else if ("Content-Length".equals(name)) {
            try {
                contentLength = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // todo: can we log it somehow plz?
            }
        }
    }

    @Override
    public void setHeader(String name, String value) {
        super.setHeader(name, value);
        if ("Content-Encoding".equals(name)) {
            contentEncoding = value;
        } else if ("Content-Length".equals(name)) {
            try {
                contentLength = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // todo: can we log it somehow plz?
            }
        }
    }

    @Override
    public void setIntHeader(String name, int value) {
        super.setIntHeader(name, value);
        if ("Content-Length".equals(name)) {
            contentLength = value;
        }
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    private String contentType;

    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        super.setContentType(this.contentType = contentType);
    }

    // headers relates methods

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot set error status - response is already committed");
        }
        notifyBeforeCommit();
        super.sendError(sc, msg);
        setCommitted();
    }

    @Override
    public void sendError(int sc) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot set error status - response is already committed");
        }
        notifyBeforeCommit();
        super.sendError(sc);
        setCommitted();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot set error status - response is already committed");
        }
        notifyBeforeCommit();
        super.sendRedirect(location);
        setCommitted();
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
        if (isCommitted()) {
            throw new IllegalStateException("Cannot reset buffer - response is already committed");
        }
        if (null != bufferedServletOutputStream) bufferedServletOutputStream.reset();
    }

    @Override
    public boolean isCommitted() {
        return committed;
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
            return outputStream = getBufferedServletOutputStream() ;
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (null != writer) {
            return writer;
        } else if (null != outputStream) {
            throw new IllegalStateException("getOutputStream() method has been called on this response");
        } else {
            return writer = new PrintWriter(new OutputStreamWriter(getBufferedServletOutputStream()), false);
        }
    }

}
