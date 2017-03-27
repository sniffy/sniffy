package io.sniffy.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import static io.sniffy.servlet.SniffyFilter.*;
import static java.lang.String.format;

/**
 * @see SniffyFilter
 * @since 2.3.0
 */
class BufferedServletResponseWrapper extends HttpServletResponseWrapper {

    private BufferedServletOutputStream bufferedServletOutputStream;

    private BufferedServletOutputStream outputStream;
    private BufferedPrintWriter writer;

    private boolean committed;
    private long contentLength;
    private String characterEncoding;
    private boolean corsHeadersHeaderAdded = false;

    private final BufferedServletResponseListener servletResponseListener;

    protected BufferedServletResponseWrapper(HttpServletResponse response,
                                             BufferedServletResponseListener servletResponseListener) {
        super(response);

        committed = response.isCommitted();

        String contentLengthHeader = response.getHeader("Content-Length");
        if (null != contentLengthHeader) {
            try {
                contentLength = Long.parseLong(contentLengthHeader);
            } catch (Exception e) {
                // TODO: log me maybe
            }
        }

        contentType = response.getContentType();
        characterEncoding = response.getCharacterEncoding();

        String corsHeadersHeader = response.getHeader(HEADER_CORS_HEADERS);
        if (null != corsHeadersHeader && corsHeadersHeader.contains(HEADER_NUMBER_OF_QUERIES)) {
            corsHeadersHeaderAdded = true;
        }

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

        // TODO: do not flush if wasn't requested by underlying application
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

    @Override
    public void setContentLengthLong(long len) {
        super.setContentLengthLong(len);
        this.contentLength = len;
    }

    public long getContentLength() {
        return contentLength;
    }

    protected void addCorsHeadersHeaderIfRequired() {
        if (!corsHeadersHeaderAdded) {
            super.setHeader(HEADER_CORS_HEADERS, format("%s, %s, %s", HEADER_NUMBER_OF_QUERIES, HEADER_REQUEST_DETAILS, HEADER_TIME_TO_FIRST_BYTE));
        }
    }

    @Override
    public void addHeader(String name, String value) {
        String processedValue = addCorsHeadersIfNecessary(name, value);
        super.addHeader(name, processedValue);
        processSpecialHeader(name, processedValue);
    }

    @Override
    public void setHeader(String name, String value) {
        String processedValue = addCorsHeadersIfNecessary(name, value);
        super.setHeader(name, processedValue);
        processSpecialHeader(name, processedValue);

    }

    private void processSpecialHeader(String name, String processedValue) {
        if ("Content-Type".equals(name)) {
            String[] splits = processedValue.split(";");
            contentType = splits[0];
            for (int i = 1; i < splits.length; i++) {
                String parameter = splits[i];
                String[] keyAndValue = parameter.split("=");
                if (2 == keyAndValue.length && null != keyAndValue[0] && "charset".equalsIgnoreCase(keyAndValue[0].trim())) {
                    characterEncoding = keyAndValue[1];
                }
            }
        } else if ("Content-Length".equals(name)) {
            try {
                contentLength = Long.parseLong(processedValue);
            } catch (NumberFormatException e) {
                // sniffy is not interested in this exception
            }
        }
    }

    private String addCorsHeadersIfNecessary(String name, String value) {
        String processedValue = value;
        if (HEADER_CORS_HEADERS.equals(name)) {
            processedValue = format("%s, %s, %s, %s", HEADER_NUMBER_OF_QUERIES, HEADER_REQUEST_DETAILS, HEADER_TIME_TO_FIRST_BYTE, processedValue);
            corsHeadersHeaderAdded = true;
        }
        return processedValue;
    }

    @Override
    public void setIntHeader(String name, int value) {
        super.setIntHeader(name, value);
        if ("Content-Length".equals(name)) {
            contentLength = value;
        }
    }

    @Override
    public void addIntHeader(String name, int value) {
        super.addIntHeader(name, value);
        if ("Content-Length".equals(name)) {
            contentLength = value;
        }
    }

    @Override
    public void setCharacterEncoding(String charset) {
        super.setCharacterEncoding(charset);
        this.characterEncoding = charset;
    }

    @Override
    public void setLocale(Locale loc) {
        super.setLocale(loc);
        characterEncoding = getCharacterEncoding();
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
        // TODO: super.flushBuffer()
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
            writer = new BufferedPrintWriter(getBufferedServletOutputStream(), characterEncoding);
            return writer;
        }
    }

}
