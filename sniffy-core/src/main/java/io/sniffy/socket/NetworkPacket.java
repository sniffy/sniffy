package io.sniffy.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 3.1.10
 */
public class NetworkPacket {

    private final boolean sent;
    private final long timestamp;
    private final Protocol protocol; // TODO: move to SocketMetaData
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public NetworkPacket(boolean sent, long timestamp, Protocol protocol, byte[] traffic, int off, int len) {
        this.sent = sent;
        this.timestamp = timestamp;
        this.protocol = protocol;
        this.baos.write(traffic, off, len);
    }

    public boolean combine(boolean sent, long timestamp, Protocol protocol, byte[] traffic, int off, int len, long maxDelay) {
        if (this.sent != sent) return false;
        if (this.protocol != protocol) return false;
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

    public Protocol getProtocol() {
        return protocol;
    }

    public byte[] getBytes() {
        return baos.toByteArray();
    }
}
