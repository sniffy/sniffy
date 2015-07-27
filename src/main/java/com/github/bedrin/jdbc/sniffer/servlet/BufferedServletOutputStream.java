package com.github.bedrin.jdbc.sniffer.servlet;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

class BufferedServletOutputStream extends ServletOutputStream {

    private final BufferedServletResponseWrapper responseWrapper;

    private final OutputStream target;
    private final Buffer buffer = new Buffer();

    private boolean closed;
    private boolean flushed;

    public BufferedServletOutputStream(BufferedServletResponseWrapper responseWrapper, OutputStream target) {
        this.responseWrapper = responseWrapper;
        this.target = target;
    }

    public void doFlush() throws IOException {
        responseWrapper.notifyBeforeFlush();
        buffer.writeTo(target);
        if (isFlushed()) target.flush();
    }

    public void setBufferSize(int size) {
        checkNotFlushed();
        buffer.ensureCapacity(size);
    }

    public int getBufferSize() {
        return buffer.getCapacity();
    }

    @Override
    public void close() throws IOException {
        flush();
        closed = true;
    }

    public void checkOpen() throws IOException {
        if (closed) throw new IOException("Output Stream is closed");
    }

    public void checkNotFlushed() throws IllegalStateException {
        if (flushed) throw new IllegalStateException("Output Stream was already sent to client");
    }

    public boolean isFlushed() {
        return flushed;
    }

    // delegate all calls to buffer

    @Override
    public void write(int b) throws IOException {
        checkOpen();
        buffer.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkOpen();
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkOpen();
        buffer.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkOpen();
        flushed = true;
        responseWrapper.setCommitted();
    }

    public void reset() {
        checkNotFlushed();
        buffer.reset();
    }

    private static class Buffer extends ByteArrayOutputStream {

        public int getCapacity() {
            return null == buf ? 0 : buf.length;
        }

        /**
         * Increases the capacity if necessary to ensure that it can hold
         * at least the number of elements specified by the minimum
         * capacity argument.
         *
         * @param minCapacity the desired minimum capacity
         * @throws OutOfMemoryError if {@code minCapacity < 0}.  This is
         * interpreted as a request for the unsatisfiably large capacity
         * {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}.
         */
        private void ensureCapacity(int minCapacity) {
            // overflow-conscious code
            if (minCapacity - buf.length > 0)
                grow(minCapacity);
        }

        /**
         * Increases the capacity to ensure that it can hold at least the
         * number of elements specified by the minimum capacity argument.
         *
         * @param minCapacity the desired minimum capacity
         */
        private void grow(int minCapacity) {
            // overflow-conscious code
            int oldCapacity = buf.length;
            int newCapacity = oldCapacity << 1;
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity;
            if (newCapacity < 0) {
                if (minCapacity < 0) // overflow
                    throw new OutOfMemoryError();
                newCapacity = Integer.MAX_VALUE;
            }
            buf = Arrays.copyOf(buf, newCapacity);
        }

    }

}
