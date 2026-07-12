package io.sniffy.socket;

import java.net.ConnectException;
import java.net.InetSocketAddress;

/**
 * @since 3.1.7
 */
public interface SniffyNetworkConnection extends TrafficCapturingNetworkConnection {

    int DEFAULT_TCP_WINDOW_SIZE = 212992;

    InetSocketAddress getInetSocketAddress();

    /**
     * Applies an endpoint-less or wildcard registry update to this connection's current effective endpoint.
     */
    void setConnectionStatus(Integer connectionStatus);

    /** Endpoint-aware registry callback; implementations with one endpoint may use the default. */
    default void setConnectionStatus(InetSocketAddress endpoint, Integer connectionStatus) {
        setConnectionStatus(connectionStatus);
    }

    /**
     * @since 3.1.13
     */
    void setProxiedInetSocketAddress(InetSocketAddress proxiedAddress);

    /** Atomically publishes a detected proxy target and the policy resolved for that target. */
    default void setProxiedInetSocketAddressAndStatus(InetSocketAddress proxiedAddress, Integer connectionStatus) {
        setProxiedInetSocketAddress(proxiedAddress);
        setConnectionStatus(connectionStatus);
    }

    /**
     * @since 3.1.13
     */
    InetSocketAddress getProxiedInetSocketAddress();

    /**
     * @since 3.1.13
     */
    void  setFirstPacketSent(boolean firstPacketSent);

    /**
     * @since 3.1.13
     */
    boolean isFirstPacketSent();

    int getPotentiallyBufferedInputBytes();

    void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes);

    int getPotentiallyBufferedOutputBytes();

    void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes);

    long getLastReadThreadId();

    void setLastReadThreadId(long lastReadThreadId);

    long getLastWriteThreadId();

    void setLastWriteThreadId(long lastWriteThreadId);

    @Deprecated
    void logSocket(long millis);

    @Deprecated
    void logSocket(long millis, int bytesDown, int bytesUp);

    void checkConnectionAllowed() throws ConnectException;

    void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException;

    void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException;

    void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException;

}
