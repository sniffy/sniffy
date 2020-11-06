package io.sniffy.socket;

import io.sniffy.Sniffy;

import java.net.ConnectException;
import java.net.InetSocketAddress;

public interface SniffyNetworkConnection {

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

    int getReceiveBufferSize();

    void setReceiveBufferSize(int receiveBufferSize);

    int getSendBufferSize();

    void setSendBufferSize(int sendBufferSize);

    void logSocket(long millis);

    void logSocket(long millis, int bytesDown, int bytesUp);

    void checkConnectionAllowed() throws ConnectException;

    void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException;

    void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException;

    void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException;

}
