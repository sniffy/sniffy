package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.SpyConfiguration;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.registry.ConnectionsRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SniffySocket extends SniffySocketAdapter implements SniffyNetworkConnection {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySocket.class);

    private final SocketChannel socketChannel;

    private InetSocketAddress address;

    private final int id;

    // fields related to injecting latency fault
    private volatile int potentiallyBufferedInputBytes = 0;
    private volatile int potentiallyBufferedOutputBytes = 0;

    private volatile long lastReadThreadId;
    private volatile long lastWriteThreadId;

    private volatile Integer connectionStatus;

    private boolean firstChunk = true;

    private final Sleep sleep = new Sleep();

    public SniffySocket(Socket delegate, SocketChannel socketChannel, int connectionId, InetSocketAddress address) throws SocketException {
        super(delegate);
        this.socketChannel = socketChannel;
        this.id = connectionId;
        if (null == address) {
            this.address = (InetSocketAddress) delegate.getRemoteSocketAddress();
        } else {
            this.address = address;
        }
    }

    @Override
    public void setConnectionStatus(Integer connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        return this.address;
    }

    private boolean firstPacketSent;
    private InetSocketAddress proxiedAddress;

    @Override
    public void setProxiedInetSocketAddress(InetSocketAddress proxiedAddress) {
        this.proxiedAddress = proxiedAddress;
    }

    @Override
    public InetSocketAddress getProxiedInetSocketAddress() {
        return proxiedAddress;
    }

    @Override
    public void setFirstPacketSent(boolean firstPacketSent) {
        this.firstPacketSent = firstPacketSent;
    }

    @Override
    public boolean isFirstPacketSent() {
        return firstPacketSent;
    }

    @Deprecated
    @Override
    public void logSocket(long millis) {
        logSocket(millis, 0, 0);
    }

    @Deprecated
    @Override
    public void logSocket(long millis, int bytesDown, int bytesUp) {
        Sniffy.SniffyMode sniffyMode = Sniffy.getSniffyMode();
        if (sniffyMode.isEnabled() && null != address && (millis > 0 || bytesDown > 0 || bytesUp > 0)) {
            Sniffy.logSocket(id, address, millis, bytesDown, bytesUp, sniffyMode.isCaptureStackTraces());
        }
    }

    public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            LOG.trace("SniffySocket.logTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + id);
            Sniffy.logTraffic(
                    id, address,
                    sent, protocol,
                    traffic, off, len,
                    effectiveSpyConfiguration.isCaptureStackTraces()
            );
            if (sent && firstChunk) {
                SniffySSLNetworkConnection sniffySSLNetworkConnection = Sniffy.CLIENT_HELLO_CACHE.get(ByteBuffer.wrap(traffic, off, len));
                if (null != sniffySSLNetworkConnection) {
                    sniffySSLNetworkConnection.setSniffyNetworkConnection(this);
                }
            }
            firstChunk = false;
        }
    }

    public void logDecryptedTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            LOG.trace("SniffySocket.logDecryptedTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + id);
            Sniffy.logDecryptedTraffic(
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
                    sleep.doSleep(-1 * connectionStatus * numberOfSleepCycles);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                throw new ConnectException(String.format("Connection to %s refused by Sniffy", inetSocketAddress));
            } else if (numberOfSleepCycles > 0 && connectionStatus > 0) {
                try {
                    sleep.doSleep(connectionStatus * numberOfSleepCycles);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
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
        checkConnectionAllowed();
        try {
            return new SnifferOutputStream(this, super.getOutputStream());
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed(1);
            super.sendUrgentData(data);
        } finally {
            logSocket(System.currentTimeMillis() - start, 0, 1);
            logTraffic(true, Protocol.TCP, new byte[]{(byte) data}, 0, 1);
        }
    }

    // TODO: evaluate other methods

    // TODO: move methods below to JAva8+ only implementation

    /*@Override
    public <T> Socket setOption(SocketOption<T> name, T value) throws IOException {
        return super.setOption(name, value);
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return super.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return super.supportedOptions();
    }*/


    //

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
