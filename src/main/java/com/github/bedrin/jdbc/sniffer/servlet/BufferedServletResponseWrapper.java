package com.github.bedrin.jdbc.sniffer.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

class BufferedServletResponseWrapper extends HttpServletResponseWrapper {

    private Collection<FlushResponseListener> flushListeners = new ArrayList<FlushResponseListener>();
    private Collection<CloseResponseListener> closeListeners = new ArrayList<CloseResponseListener>();

    private BufferedServletOutputStream bufferedServletOutputStream;
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    private boolean committed;

    private final HttpServletResponse delegate;

    protected BufferedServletResponseWrapper(HttpServletResponse response) {
        super(response);
        delegate = response;
    }

    protected void addFlushResponseListener(FlushResponseListener listener) {
        flushListeners.add(listener);
    }

    protected void addCloseResponseListener(CloseResponseListener listener) {
        closeListeners.add(listener);
    }

    protected void notifyBeforeFlush() throws IOException {
        Iterator<FlushResponseListener> listenersIt = flushListeners.iterator();
        while (listenersIt.hasNext()) {
            FlushResponseListener listener = listenersIt.next();
            listenersIt.remove();
            listener.beforeFlush(delegate, this);
        }
    }

    protected void notifyBeforeClose() throws IOException {
        Iterator<CloseResponseListener> listenersIt = closeListeners.iterator();
        while (listenersIt.hasNext()) {
            CloseResponseListener listener = listenersIt.next();
            listenersIt.remove();
            listener.beforeClose(delegate, this);
        }
    }

    protected BufferedServletOutputStream getBufferedServletOutputStream() throws IOException {
        if (null == bufferedServletOutputStream) {
            bufferedServletOutputStream = new BufferedServletOutputStream(this, super.getOutputStream());
        }
        return bufferedServletOutputStream;
    }

    protected void flush() throws IOException {
        notifyBeforeFlush();
        if (null != writer) writer.flush();
        if (null != outputStream) outputStream.flush();
        if (null != bufferedServletOutputStream) bufferedServletOutputStream.closeTarget();
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
        notifyBeforeFlush();
        super.sendError(sc, msg);
        setCommitted();
    }

    @Override
    public void sendError(int sc) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot set error status - response is already committed");
        }
        notifyBeforeFlush();
        super.sendError(sc);
        setCommitted();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot set error status - response is already committed");
        }
        notifyBeforeFlush();
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
            return writer = new PrintWriter(getBufferedServletOutputStream());
        }
    }

}
