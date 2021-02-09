package io.sniffy.socket;

import java.io.ByteArrayOutputStream;

/**
 * @since 3.1.10
 */
public class NetworkPacket implements Comparable<NetworkPacket> {

    private final boolean sent;
    private final long timestamp;
    private final String stackTrace;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public NetworkPacket(boolean sent, long timestamp, String stackTrace, byte[] traffic, int off, int len) {
        this.sent = sent;
        this.timestamp = timestamp;
        this.stackTrace = stackTrace;
        this.baos.write(traffic, off, len);
    }

    public boolean combine(boolean sent, long timestamp, String stackTrace, byte[] traffic, int off, int len, long maxDelay) {
        if (this.sent != sent) return false;
        if (timestamp - this.timestamp > maxDelay) return false;
        //noinspection StringEquality
        if (this.stackTrace != stackTrace) return false;
        //noinspection ConstantConditions
        if (null != this.stackTrace && !this.stackTrace.equals(stackTrace)) return false;
        this.baos.write(traffic, off, len);
        return true;
    }

    public boolean combine(NetworkPacket that, long maxDelay) {
        if (this.sent != that.sent) return false;
        if (that.timestamp - this.timestamp > maxDelay) return false;
        //noinspection StringEquality
        if (this.stackTrace != that.stackTrace) return false;
        //noinspection ConstantConditions
        if (null != this.stackTrace && !this.stackTrace.equals(that.stackTrace)) return false;
        byte[] bytes = that.getBytes();
        this.baos.write(bytes, 0, bytes.length);
        return true;
    }

    public boolean isSent() {
        return sent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public byte[] getBytes() {
        return baos.toByteArray();
    }

    @Override
    public int compareTo(NetworkPacket that) {
        //noinspection UseCompareMethod
        return (timestamp < that.timestamp) ? -1 : ((timestamp == that.timestamp) ? 0 : 1);
    }

}
