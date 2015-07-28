package com.github.bedrin.jdbc.sniffer.servlet;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

class BufferedServletOutputStream extends ServletOutputStream {

    private final BufferedServletResponseWrapper responseWrapper;

    private final ServletOutputStream target;
    private final Buffer buffer = new Buffer();

    private boolean closed;
    private boolean flushed;

    protected BufferedServletOutputStream(BufferedServletResponseWrapper responseWrapper, ServletOutputStream target) {
        this.responseWrapper = responseWrapper;
        this.target = target;
    }

    @Override
    public void flush() throws IOException {
        flushed = true;

        responseWrapper.notifyBeforeFlush();

        buffer.writeTo(target);
        target.flush();

        buffer.reset();
        responseWrapper.setCommitted();
    }

    public void closeTarget() throws IOException {
        responseWrapper.notifyBeforeClose();
        if (closed) target.close();
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

    @Override
    public void close() throws IOException {
        flush();
        responseWrapper.notifyBeforeClose();
        closed = true;
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

    // TODO: flush buffer automatically after some threshold (say 100 kilobytes for start?) or analyze content-length headedr
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
