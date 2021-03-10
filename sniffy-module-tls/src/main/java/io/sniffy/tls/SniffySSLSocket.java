package io.sniffy.tls;

import io.sniffy.Sniffy;
import io.sniffy.SpyConfiguration;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SnifferInputStream;
import io.sniffy.socket.SnifferOutputStream;
import io.sniffy.socket.SniffyNetworkConnection;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class SniffySSLSocket extends SSLSocketAdapter implements SniffyNetworkConnection {

    // TODO: cover all methods with unit tests

    private final SocketChannel socketChannel; // TODO: support

    private InetSocketAddress address;

    //
    private final static AtomicInteger counter = new AtomicInteger(); // TODO: make global counter

    private final int id = counter.getAndIncrement(); // TODO: make global;

    protected static volatile Integer defaultReceiveBufferSize;
    protected static volatile Integer defaultSendBufferSize;

    private int receiveBufferSize = -1;
    private int sendBufferSize = -1;

    private volatile int potentiallyBufferedInputBytes = 0;
    private volatile int potentiallyBufferedOutputBytes = 0;

    private volatile long lastReadThreadId;
    private volatile long lastWriteThreadId;

    private volatile Integer connectionStatus;

    @Override
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    @Override
    public int getSendBufferSize() {
        return sendBufferSize;
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    @Override
    public int getPotentiallyBufferedInputBytes() {
        return potentiallyBufferedInputBytes;
    }

    @Override
    public void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes) {
        this.potentiallyBufferedInputBytes = potentiallyBufferedInputBytes;
    }

    @Override
    public int getPotentiallyBufferedOutputBytes() {
        return potentiallyBufferedOutputBytes;
    }

    @Override
    public void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes) {
        this.potentiallyBufferedOutputBytes = potentiallyBufferedOutputBytes;
    }

    @Override
    public long getLastReadThreadId() {
        return lastReadThreadId;
    }

    @Override
    public void setLastReadThreadId(long lastReadThreadId) {
        this.lastReadThreadId = lastReadThreadId;
    }

    @Override
    public long getLastWriteThreadId() {
        return lastWriteThreadId;
    }

    @Override
    public void setLastWriteThreadId(long lastWriteThreadId) {
        this.lastWriteThreadId = lastWriteThreadId;
    }


    @Override
    public void logSocket(long millis) {
        // TODO: refactor
        // NOOP since it's done by underlying SniffySocketImpl
    }

    @Override
    public void logSocket(long millis, int bytesDown, int bytesUp) {
        // TODO: refactor
        // NOOP since it's done by underlying SniffySocketImpl
    }

    @Override
    public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            Sniffy.logDecryptedTraffic(
                    id, address,
                    sent, protocol,
                    traffic, off, len,
                    effectiveSpyConfiguration.isCaptureStackTraces()
            );
        }
    }

    @Override
    public void checkConnectionAllowed() throws ConnectException {
        // TODO: refactor
        // NOOP since it's done by underlying SniffySocketImpl
    }

    @Override
    public void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException {
        // TODO: refactor
        // NOOP since it's done by underlying SniffySocketImpl
    }

    @Override
    public void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException {
        // TODO: refactor
        // NOOP since it's done by underlying SniffySocketImpl
    }

    @Override
    public void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException {
        // TODO: refactor
        // NOOP since it's done by underlying SniffySocketImpl
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        // TODO: refactor
        return address;
    }

    @Override
    public void setConnectionStatus(Integer connectionStatus) {
        // TODO: refactor
        // NOOP since it's done by underlying SniffySocketImpl
    }

    //

    public SniffySSLSocket(Socket delegate, InetSocketAddress address) {
        this((SSLSocket) delegate, address); // TODO: support when we cannot cast
    }

    public SniffySSLSocket(SSLSocket delegate, InetSocketAddress address) {
        super(delegate);
        this.socketChannel = null;
        if (null == address) {
            this.address = (InetSocketAddress) delegate.getRemoteSocketAddress();
        } else {
            this.address = address;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new SnifferInputStream(this, super.getInputStream());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new SnifferOutputStream(this, super.getOutputStream());
    }

    // TODO: override other methods like sendUrgentData, connect, etc.

}
