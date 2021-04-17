package io.sniffy.socket;

import java.net.ConnectException;
import java.net.InetSocketAddress;

/**
 * @since 3.1.7
 */
public interface SniffyNetworkConnection extends TrafficCapturingNetworkConnection {

    int DEFAULT_TCP_WINDOW_SIZE = 212992;

    InetSocketAddress getInetSocketAddress();

    void setConnectionStatus(Integer connectionStatus);

    int getPotentiallyBufferedInputBytes();

    void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes);

    int getPotentiallyBufferedOutputBytes();

    void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes);

    long getLastReadThreadId();

    void setLastReadThreadId(long lastReadThreadId);

    long getLastWriteThreadId();

    void setLastWriteThreadId(long lastWriteThreadId);

    void logSocket(long millis);

    @Deprecated
    void logSocket(long millis, int bytesDown, int bytesUp);

    void checkConnectionAllowed() throws ConnectException;

    void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException;

    void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException;

    void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException;

}
