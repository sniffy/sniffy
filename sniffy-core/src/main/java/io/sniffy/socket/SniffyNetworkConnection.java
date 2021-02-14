package io.sniffy.socket;

import java.net.ConnectException;
import java.net.InetSocketAddress;

/**
 * @since 3.1.7
 */
public interface SniffyNetworkConnection {

    InetSocketAddress getInetSocketAddress();

    void setConnectionStatus(Integer connectionStatus);

    void setProxiedInetSocketAddress(InetSocketAddress proxiedAddress);

    InetSocketAddress getProxiedInetSocketAddress();

    void  setFirstPacketSent(boolean firstPacketSent);

    boolean isFirstPacketSent();

    int getPotentiallyBufferedInputBytes();

    void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes);

    int getPotentiallyBufferedOutputBytes();

    void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes);

    long getLastReadThreadId();

    void setLastReadThreadId(long lastReadThreadId);

    long getLastWriteThreadId();

    void setLastWriteThreadId(long lastWriteThreadId);

    int getReceiveBufferSize();

    void setReceiveBufferSize(int receiveBufferSize);

    int getSendBufferSize();

    void setSendBufferSize(int sendBufferSize);

    void logSocket(long millis);

    @Deprecated
    void logSocket(long millis, int bytesDown, int bytesUp);

    // TODO: add
    void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len);

    void checkConnectionAllowed() throws ConnectException;

    void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException;

    void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException;

    void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException;

}
