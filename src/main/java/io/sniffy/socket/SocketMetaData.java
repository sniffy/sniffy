package io.sniffy.socket;

import java.net.InetSocketAddress;

/**
 * Created by bedrin on 29.04.2016.
 */
public class SocketMetaData {

    public final InetSocketAddress address;
    public final int connectionId;
    public final String stackTrace;
    public final Thread owner;

    public SocketMetaData(InetSocketAddress address, int connectionId, String stackTrace, Thread owner) {
        this.address = address;
        this.connectionId = connectionId;
        this.stackTrace = stackTrace;
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocketMetaData that = (SocketMetaData) o;

        if (connectionId != that.connectionId) return false;
        if (!address.equals(that.address)) return false;
        if (stackTrace != null ? !stackTrace.equals(that.stackTrace) : that.stackTrace != null) return false;
        return owner.equals(that.owner);

    }

    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + connectionId;
        result = 31 * result + (stackTrace != null ? stackTrace.hashCode() : 0);
        result = 31 * result + owner.hashCode();
        return result;
    }
}
