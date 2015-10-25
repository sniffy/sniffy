package com.github.bedrin.jdbc.sniffer.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * TODO: flush buffer automatically after some threshold (say 100 kilobytes for start?) or analyze content-length headedr
 */
class Buffer extends ByteArrayOutputStream {

    public InputStream reverseInputStream() {
        return new InputStream() {

            private int pos = count;

            @Override
            public int read() throws IOException {
                return 0 == pos ? -1 : buf[--pos];
            }

        };
    }

    public synchronized byte[] toByteArray(int maxSize) {
        return Arrays.copyOf(buf, Math.max(count, maxSize));
    }

    public void insertAt(int pos, byte[] data) {
        ensureCapacity(count + data.length);
        System.arraycopy(buf, pos, buf, pos + data.length, count - pos);
        System.arraycopy(data, 0, buf, pos, data.length);
        count += data.length;
    }

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
     *                          interpreted as a request for the unsatisfiably large capacity
     *                          {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}.
     */
    public void ensureCapacity(int minCapacity) {
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
