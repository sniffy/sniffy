package io.sniffy.servlet.internal;

import java.io.ByteArrayOutputStream;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

/**
 * Namespace-neutral response buffer shared by the Servlet integrations.
 */
public class Buffer extends ByteArrayOutputStream {

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

    public void ensureCapacity(int minCapacity) {
        if (minCapacity - buf.length > 0) {
            grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        if (newCapacity < 0) {
            if (minCapacity < 0) {
                throw new OutOfMemoryError();
            }
            newCapacity = Integer.MAX_VALUE;
        }
        buf = copyOf(buf, newCapacity);
    }
}
