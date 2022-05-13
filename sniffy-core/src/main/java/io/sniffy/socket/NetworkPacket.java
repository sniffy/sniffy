package io.sniffy.socket;

import io.sniffy.ThreadMetaData;
import io.sniffy.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * @since 3.1.10
 */
public class NetworkPacket {

    private final boolean sent;
    private final long timestamp;

    private final String stackTrace;
    private final ThreadMetaData threadMetaData;

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public NetworkPacket(boolean sent, long timestamp, String stackTrace, ThreadMetaData threadMetaData, byte[] traffic, int off, int len) {
        this.sent = sent;
        this.timestamp = timestamp;
        this.stackTrace = stackTrace;
        this.threadMetaData = threadMetaData;
        this.baos.write(traffic, off, len);
    }

    public boolean combine(boolean sent, long timestamp, String stackTrace, ThreadMetaData threadMetaData, byte[] traffic, int off, int len, long maxDelay) {
        if (this.sent != sent) return false;
        if (timestamp - this.timestamp > maxDelay) return false;
        //noinspection StringEquality
        if (this.stackTrace != stackTrace) return false;
        //noinspection ConstantConditions
        if (null != this.stackTrace && !this.stackTrace.equals(stackTrace)) return false;
        if (null != this.threadMetaData && !this.threadMetaData.equals(threadMetaData)) return false;
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
        if (null != this.threadMetaData && !this.threadMetaData.equals(that.threadMetaData)) return false;
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

    public ThreadMetaData getThreadMetaData() {
        return threadMetaData;
    }

    public byte[] getBytes() {
        return baos.toByteArray();
    }

    /**
     * @since 3.1.13
     */
    public static String convertNetworkPacketsToString(List<NetworkPacket> packets) {
        StringBuilder sb = new StringBuilder();
        for (NetworkPacket packet : packets) {
            sb.append(packet.getTimestamp()).
                    append(" isSent=").
                    append(packet.isSent()).
                    append(" content=").
                    append(StringUtil.LINE_SEPARATOR).
                    append(new String(packet.getBytes())).
                    append(StringUtil.LINE_SEPARATOR);
        }
        return sb.toString();
    }

}
