package io.sniffy.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;

/**
 * @see SniffyFilter
 * @since 2.3.0
 */
class BufferedServletOutputStream extends ServletOutputStream {

    private final BufferedServletResponseWrapper responseWrapper;

    private final ServletOutputStream target;
    private final Buffer buffer = new Buffer();

    private boolean closed;
    private boolean flushed;

    private boolean lastChunk;

    public void setLastChunk() {
        this.lastChunk = true;
    }

    protected BufferedServletOutputStream(BufferedServletResponseWrapper responseWrapper, ServletOutputStream target) {
        this.responseWrapper = responseWrapper;
        this.target = target;
    }

    public void flushIfOpen() throws IOException {
        if (!closed) {
            flush();
        }
    }

    @Override
    public void flush() throws IOException {

        if (!flushed) {
            responseWrapper.notifyBeforeCommit(buffer);
        }

        if (lastChunk) {
            responseWrapper.notifyBeforeClose(buffer);
            lastChunk = false;
        }

        buffer.writeTo(target);
        target.flush();

        buffer.reset();
        responseWrapper.setCommitted();

        flushed = true;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            setLastChunk();
            flush();
            target.close();
            closed = true;
        }
    }

    public void reset() {
        checkNotFlushed();
        buffer.reset();
    }

    protected void setBufferSize(int size) {
        checkNotFlushed();
        buffer.ensureCapacity(size);
    }

    protected int getBufferSize() {
        return buffer.getCapacity();
    }

    protected void checkOpen() throws IOException {
        if (closed) throw new IOException("Output Stream is closed");
    }

    protected void checkNotFlushed() throws IllegalStateException {
        if (flushed) throw new IllegalStateException("Output Stream was already sent to client");
    }

    // delegate all calls to buffer

    @Override
    public void write(int b) throws IOException {
        checkOpen();
        flushIfOverflow(1);
        buffer.write(b);
    }

    private int maximumBufferSize = 200 * 1024;

    private void flushIfOverflow(int newBytes) throws IOException {
        if (buffer.size() + newBytes > maximumBufferSize) {
            flush(); // TODO: do not flush the whole buffer but only say the first half
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkOpen();
        flushIfOverflow(b.length);
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkOpen();
        flushIfOverflow(len);
        buffer.write(b, off, len);
    }

    // TODO: add tests with different servlet api versions (2.5, 3.0.1, 3.1.0, 4.0-betas)

    @Override
    public boolean isReady() {
        return target.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        target.setWriteListener(writeListener);
    }

}
