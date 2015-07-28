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

    private Collection<ServletResponseListener> listeners = new ArrayList<ServletResponseListener>();

    private BufferedServletOutputStream bufferedServletOutputStream;
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    private boolean committed;

    private final HttpServletResponse delegate;

    protected BufferedServletResponseWrapper(HttpServletResponse response) {
        super(response);
        delegate = response;
    }

    protected void addServletResponseListener(ServletResponseListener listener) {
        listeners.add(listener);
    }

    protected void notifyBeforeFlush() throws IOException {
        Iterator<ServletResponseListener> listenersIt = listeners.iterator();
        while (listenersIt.hasNext()) {
            ServletResponseListener listener = listenersIt.next();
            listener.beforeFlush(delegate);
            listenersIt.remove();
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
