package io.sniffy.socket;

import io.sniffy.ThreadMetaData;

import java.net.InetSocketAddress;

/**
 * @since 3.1
 */
public class SocketMetaData {

    private final Protocol protocol;

    @Deprecated
    public final InetSocketAddress address;
    @Deprecated
    public final int connectionId;
    @Deprecated
    public final String stackTrace;
    @Deprecated
    public final long ownerThreadId;
    private final ThreadMetaData threadMetaData;

    private final int hashCode;

    public SocketMetaData(Protocol protocol, InetSocketAddress address, int connectionId) {
        this(protocol, address, connectionId, null, (ThreadMetaData) null);
    }

    public SocketMetaData(InetSocketAddress address, int connectionId, String stackTrace, Thread ownerThread) {
        this(Protocol.TCP, address, connectionId, stackTrace, new ThreadMetaData(ownerThread));
    }

    public SocketMetaData(Protocol protocol, InetSocketAddress address, int connectionId, String stackTrace, Thread ownerThread) {
        this(protocol, address, connectionId, stackTrace, null == ownerThread ? null : new ThreadMetaData(ownerThread));
    }

    public SocketMetaData(Protocol protocol, InetSocketAddress address, int connectionId, String stackTrace, ThreadMetaData threadMetaData) {
        this.protocol = protocol;
        this.address = address;
        this.connectionId = connectionId;
        this.stackTrace = null == stackTrace ? null : stackTrace.intern();
        this.threadMetaData = threadMetaData;
        this.ownerThreadId = null == threadMetaData ? -1 : threadMetaData.getThreadId();
        hashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = address.hashCode();
        result = 31 * result + protocol.hashCode();
        result = 31 * result + connectionId;
        result = 31 * result + System.identityHashCode(stackTrace);
        result = 31 * result + (int)(ownerThreadId ^ (ownerThreadId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocketMetaData that = (SocketMetaData) o;

        if (connectionId != that.connectionId) return false;
        if (ownerThreadId != that.ownerThreadId) return false;
        if (!protocol.equals(that.protocol)) return false;
        if (!address.equals(that.address)) return false;
        //noinspection StringEquality
        return stackTrace == that.stackTrace;

    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * @since 3.1.10
     */
    public Protocol getProtocol() {
        return protocol;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public int getConnectionId() {
        return connectionId;
    }

    @Deprecated
    public String getStackTrace() {
        return stackTrace;
    }

    public ThreadMetaData getThreadMetaData() {
        return threadMetaData;
    }
}
