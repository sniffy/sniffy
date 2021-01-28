package io.sniffy.socket;

import java.io.ByteArrayOutputStream;

/**
 * @since 3.1.10
 */
public class NetworkPacket {

    private final boolean sent;
    private final long timestamp;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public NetworkPacket(boolean sent, long timestamp, byte[] traffic, int off, int len) {
        this.sent = sent;
        this.timestamp = timestamp;
        this.baos.write(traffic, off, len);
    }

    public boolean combine(boolean sent, long timestamp, byte[] traffic, int off, int len, long maxDelay) {
        if (this.sent != sent) return false;
        if (timestamp - this.timestamp > maxDelay) return false;
        this.baos.write(traffic, off, len);
        return true;
    }

    public boolean isSent() {
        return sent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getBytes() {
        return baos.toByteArray();
    }

}
