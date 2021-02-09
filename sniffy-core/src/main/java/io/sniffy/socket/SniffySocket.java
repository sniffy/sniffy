package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.SpyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class SniffySocket extends SniffySocketAdapter implements SniffyNetworkConnection {

    private final SocketChannel socketChannel;

    private InetSocketAddress address;

    private final static AtomicInteger counter = new AtomicInteger();

    private final int id;

    protected static volatile Integer defaultReceiveBufferSize;
    protected static volatile Integer defaultSendBufferSize;

    private int receiveBufferSize = -1;
    private int sendBufferSize = -1;

    private volatile int potentiallyBufferedInputBytes = 0;
    private volatile int potentiallyBufferedOutputBytes = 0;

    private volatile long lastReadThreadId;
    private volatile long lastWriteThreadId;

    private volatile Integer connectionStatus;

    public SniffySocket(Socket delegate, SocketChannel socketChannel, int connectionId) throws SocketException {
        super(delegate);
        this.socketChannel = socketChannel;
        this.id = connectionId;
    }

    @Override
    public void setConnectionStatus(Integer connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        return this.address;
    }

    private void estimateReceiveBuffer() {
        if (-1 == receiveBufferSize) {
            if (null == defaultReceiveBufferSize) {
                synchronized (SnifferSocketImpl.class) {
                    if (null == defaultReceiveBufferSize) {
                        try {
                            defaultReceiveBufferSize = super.getReceiveBufferSize();
                        } catch (SocketException e) {
                            defaultReceiveBufferSize = 0;
                        }
                    }
                }
            }
            receiveBufferSize = defaultReceiveBufferSize;
        }
    }

    private void estimateSendBuffer() {
        if (-1 == sendBufferSize) {
            if (null == defaultSendBufferSize) {
                synchronized (SnifferSocketImpl.class) {
                    if (null == defaultSendBufferSize) {
                        try {
                            defaultSendBufferSize = super.getSendBufferSize();
                        } catch (SocketException e) {
                            defaultSendBufferSize = 0;
                        }
                    }
                }
            }
            sendBufferSize = defaultSendBufferSize;
        }
    }

    @Deprecated
    public void logSocket(long millis) {
        logSocket(millis, 0, 0);
    }

    @Deprecated
    public void logSocket(long millis, int bytesDown, int bytesUp) {
        Sniffy.SniffyMode sniffyMode = Sniffy.getSniffyMode();
        if (sniffyMode.isEnabled() && null != address && (millis > 0 || bytesDown > 0 || bytesUp > 0)) {
            Sniffy.logSocket(id, address, millis, bytesDown, bytesUp, sniffyMode.isCaptureStackTraces());
        }
    }

    public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            Sniffy.logTraffic(
                    id, address,
                    sent, protocol,
                    traffic, off, len,
                    effectiveSpyConfiguration.isCaptureStackTraces()
            );
        }
    }

    public void checkConnectionAllowed() throws ConnectException {
        checkConnectionAllowed(0);
    }

    public void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException {
        checkConnectionAllowed(address, numberOfSleepCycles);
    }

    public void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException {
        checkConnectionAllowed(inetSocketAddress, 1);
    }

    public void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException {
        if (null != inetSocketAddress) {
            if (null == this.connectionStatus || ConnectionsRegistry.INSTANCE.isThreadLocal()) {
                this.connectionStatus = ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(inetSocketAddress, this);
            }
            if (connectionStatus < 0) {
                if (numberOfSleepCycles > 0 && -1 != connectionStatus) try {
                    sleepImpl(-1 * connectionStatus * numberOfSleepCycles);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                throw new ConnectException(String.format("Connection to %s refused by Sniffy", inetSocketAddress));
            } else if (numberOfSleepCycles > 0 && connectionStatus > 0) {
                try {
                    sleepImpl(connectionStatus * numberOfSleepCycles);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void sleepImpl(int millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @Override
    public void connect(SocketAddress address) throws IOException {
        long start = System.currentTimeMillis();
        try {
            if (address instanceof InetSocketAddress) {
                checkConnectionAllowed(this.address = (InetSocketAddress) address);
            }
            super.connect(address);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void connect(SocketAddress address, int timeout) throws IOException {
        long start = System.currentTimeMillis();
        try {
            if (address instanceof InetSocketAddress) {
                checkConnectionAllowed(this.address = (InetSocketAddress) address);
            }
            super.connect(address, timeout);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        long start = System.currentTimeMillis();
        try {
            super.bind(bindpoint); // TODO: should we check connectivity enabled here as well ?
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public SocketChannel getChannel() {
        return null != socketChannel ? socketChannel : super.getChannel();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        long start = System.currentTimeMillis();
        estimateReceiveBuffer();
        checkConnectionAllowed();
        try {
            return new SnifferInputStream(this, super.getInputStream());
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        long start = System.currentTimeMillis();
        estimateSendBuffer();
        checkConnectionAllowed();
        try {
            return new SnifferOutputStream(this, super.getOutputStream());
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        super.sendUrgentData(data); // TODO: log traffic and do stuff
    }

    // TODO: evaluate other methods



    //


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
}
