package io.sniffy.servlet;

import java.io.ByteArrayOutputStream;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

/**
 * @see SniffyFilter
 * @since 2.3.0
 */
class Buffer extends ByteArrayOutputStream {


    public byte[] leadingBytes(int maxSize) {
        return copyOf(buf, Math.max(count, maxSize));
    }

    public byte[] trailingBytes(int maxSize) {
        return maxSize >= count ? copyOf(buf, count) : copyOfRange(buf, count - maxSize, count);
    }

    public void insertAt(int pos, byte[] data) {
        ensureCapacity(count + data.length);
        arraycopy(buf, pos, buf, pos + data.length, count - pos);
        arraycopy(data, 0, buf, pos, data.length);
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
        buf = copyOf(buf, newCapacity);
    }

}
