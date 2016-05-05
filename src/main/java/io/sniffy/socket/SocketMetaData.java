package io.sniffy.socket;

import java.net.InetSocketAddress;

/**
 * Created by bedrin on 29.04.2016.
 */
public class SocketMetaData {

    public final InetSocketAddress address;
    public final int connectionId;
    public final String stackTrace;
    public final long ownerThreadId;

    public SocketMetaData(InetSocketAddress address, int connectionId, String stackTrace, long ownerThreadId) {
        this.address = address;
        this.connectionId = connectionId;
        this.stackTrace = null == stackTrace ? null : stackTrace.intern();
        this.ownerThreadId = ownerThreadId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocketMetaData that = (SocketMetaData) o;

        if (connectionId != that.connectionId) return false;
        if (ownerThreadId != that.ownerThreadId) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        return stackTrace != null ? stackTrace.equals(that.stackTrace) : that.stackTrace == null;

    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + connectionId;
        result = 31 * result + (stackTrace != null ? stackTrace.hashCode() : 0);
        result = 31 * result + (int) (ownerThreadId ^ (ownerThreadId >>> 32));
        return result;
    }

}
