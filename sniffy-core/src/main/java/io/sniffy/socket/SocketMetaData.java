package io.sniffy.socket;

import java.net.InetSocketAddress;

/**
 * @since 3.1
 */
public class SocketMetaData {

    public final InetSocketAddress address;
    public final int connectionId;
    public final String stackTrace;
    public final long ownerThreadId;

    private final int hashCode;

    public SocketMetaData(InetSocketAddress address, int connectionId, String stackTrace, long ownerThreadId) {
        this.address = address;
        this.connectionId = connectionId;
        this.stackTrace = null == stackTrace ? null : stackTrace.intern();
        this.ownerThreadId = ownerThreadId;
        hashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = address.hashCode();
        result = 31 * result + connectionId;
        result = 31 * result + System.identityHashCode(stackTrace);
        result = 31 * result + (int) (ownerThreadId ^ (ownerThreadId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocketMetaData that = (SocketMetaData) o;

        if (connectionId != that.connectionId) return false;
        if (ownerThreadId != that.ownerThreadId) return false;
        if (!address.equals(that.address)) return false;
        return stackTrace == that.stackTrace;

    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
