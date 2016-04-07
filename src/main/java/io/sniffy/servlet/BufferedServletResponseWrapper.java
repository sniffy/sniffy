package io.sniffy.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

import static io.sniffy.servlet.SnifferFilter.*;
import static java.lang.String.format;

class BufferedServletResponseWrapper extends HttpServletResponseWrapper {

    private BufferedServletOutputStream bufferedServletOutputStream;

    private BufferedServletOutputStream outputStream;
    private BufferedPrintWriter writer;

    private boolean committed;
    private int contentLength;
    private String contentEncoding;
    private boolean corsHeadersHeaderAdded = false;

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
     * @throws IOException
     */
    protected void flushIfPossible() throws IOException {

        if (null != bufferedServletOutputStream) bufferedServletOutputStream.setLastChunk();

        if (null != writer) writer.flushIfOpen();
        else if (null != outputStream) outputStream.flushIfOpen();
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

    @Override
    public void setContentLength(int len) {
        super.setContentLength(len);
        this.contentLength = len;
    }

    public int getContentLength() {
        return contentLength;
    }

    protected void addCorsHeadersHeaderIfRequired() {
        if (!corsHeadersHeaderAdded) {
            super.setHeader(HEADER_CORS_HEADERS, format("%s, %s", HEADER_NUMBER_OF_QUERIES, HEADER_REQUEST_DETAILS, HEADER_TIME_TO_FIRST_BYTE));
        }
    }

    @Override
    public void addHeader(String name, String value) {

        String processedValue = value;
        if (HEADER_CORS_HEADERS.equals(name)) {
            processedValue = format("%s, %s, %s, %s", HEADER_NUMBER_OF_QUERIES, HEADER_REQUEST_DETAILS, HEADER_TIME_TO_FIRST_BYTE, processedValue);
            corsHeadersHeaderAdded = true;
        }

        super.addHeader(name, processedValue);
        if ("Content-Encoding".equals(name)) {
            contentEncoding = processedValue;
        } else if ("Content-Length".equals(name) && null != processedValue) {
            try {
                contentLength = Integer.parseInt(processedValue);
            } catch (NumberFormatException e) {
                // sniffy is not interested in this exception
            }
        }
    }

    @Override
    public void setHeader(String name, String value) {

        String processedValue = value;
        if (HEADER_CORS_HEADERS.equals(name)) {
            processedValue = format("%s, %s, %s, %s", HEADER_NUMBER_OF_QUERIES, HEADER_REQUEST_DETAILS, HEADER_TIME_TO_FIRST_BYTE, processedValue);
            corsHeadersHeaderAdded = true;
        }

        super.setHeader(name, processedValue);

        if ("Content-Encoding".equals(name)) {
            contentEncoding = processedValue;
        } else if ("Content-Length".equals(name)) {
            try {
                contentLength = Integer.parseInt(processedValue);
            } catch (NumberFormatException e) {
                // sniffy is not interested in this exception
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
        super.setContentType(contentType);
        this.contentType = contentType;
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
        if (null != writer) writer.flushIfOpen();
        else if (null != outputStream) outputStream.flushIfOpen();
        else {
            notifyBeforeCommit();
            setCommitted();
        }
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
            outputStream = getBufferedServletOutputStream();
            return outputStream;
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (null != writer) {
            return writer;
        } else if (null != outputStream) {
            throw new IllegalStateException("getOutputStream() method has been called on this response");
        } else {
            writer = new BufferedPrintWriter(getBufferedServletOutputStream()); // TODO: pass charset here
            return writer;
        }
    }

}
