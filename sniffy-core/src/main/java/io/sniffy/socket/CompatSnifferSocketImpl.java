package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.SpyConfiguration;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.util.StackTraceExtractor;
import io.sniffy.util.StringUtil;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * @since 3.1.9
 */
class CompatSnifferSocketImpl extends CompatSniffySocketImplAdapter implements SniffyNetworkConnection {

    private static final Polyglog LOG = PolyglogFactory.log(CompatSnifferSocketImpl.class);

    private static final Polyglog LOG_TRAFFIC_VERBOSE_LOG = PolyglogFactory.oneTimeLog(CompatSnifferSocketImpl.class);

    private final Sleep sleep;

    private InetSocketAddress address;

    private final int id = Sniffy.CONNECTION_ID_SEQUENCE.getAndIncrement();

    private volatile Integer connectionStatus;

    // fields related to injecting latency fault
    private volatile int potentiallyBufferedInputBytes = 0;
    private volatile int potentiallyBufferedOutputBytes = 0;

    private volatile long lastReadThreadId;
    private volatile long lastWriteThreadId;

    private boolean firstChunk = true;

    protected CompatSnifferSocketImpl(SocketImpl delegate, Sleep sleep) {
        super(delegate);
        this.sleep = sleep;
    }

    protected CompatSnifferSocketImpl(SocketImpl delegate) {
        this(delegate, new Sleep());
    }

    @Override
    public void setConnectionStatus(Integer connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        return this.address;
    }

    @Deprecated
    public void logSocket(long millis) {
        logSocket(millis, 0, 0);
    }

    @Deprecated
    public void logSocket(long millis, int bytesDown, int bytesUp) {

        if (!SniffyConfiguration.INSTANCE.getSocketCaptureEnabled()) return;

        if (null != address && (millis > 0 || bytesDown > 0 || bytesUp > 0)) {
            Sniffy.SniffyMode sniffyMode = Sniffy.getSniffyMode();
            if (sniffyMode.isEnabled()) {
                Sniffy.logSocket(id, address, millis, bytesDown, bytesUp, sniffyMode.isCaptureStackTraces());
            }
        }
    }

    public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            LOG.trace("CompatSnifferSocketImpl.logTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + id);
            LOG_TRAFFIC_VERBOSE_LOG.trace("StackTrace for first CompatSnifferSocketImpl.logTraffic() invocation was " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
            Sniffy.logTraffic(
                    id, getInetSocketAddress(),
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
            LOG.trace("CompatSnifferSocketImpl.logDecryptedTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + id);
            LOG_TRAFFIC_VERBOSE_LOG.trace("StackTrace for first CompatSnifferSocketImpl.logDecryptedTraffic() invocation was " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
            Sniffy.logDecryptedTraffic(
                    id, getInetSocketAddress(),
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

        if (!SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled()) return;

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

    private void sleepImpl(int millis) throws InterruptedException {
        sleep.doSleep(millis);
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed(1);
            super.sendUrgentData(data);
        } finally {
            logSocket(System.currentTimeMillis() - start, 0, 1);
            logTraffic(true, Protocol.TCP, new byte[]{(byte) data}, 0, 1);
        }
    }

    @Override
    protected void shutdownInput() throws IOException {
        long start = System.currentTimeMillis();
        try {
            super.shutdownInput();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected void shutdownOutput() throws IOException {
        long start = System.currentTimeMillis();
        try {
            super.shutdownOutput();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected FileDescriptor getFileDescriptor() {
        long start = System.currentTimeMillis();
        try {
            return super.getFileDescriptor();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected InetAddress getInetAddress() {
        long start = System.currentTimeMillis();
        try {
            return super.getInetAddress();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected int getPort() {
        long start = System.currentTimeMillis();
        try {
            return super.getPort();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected boolean supportsUrgentData() {
        long start = System.currentTimeMillis();
        try {
            return super.supportsUrgentData();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected int getLocalPort() {
        long start = System.currentTimeMillis();
        try {
            return super.getLocalPort();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public String toString() {
        long start = System.currentTimeMillis();
        try {
            return super.toString();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    // TODO: wrap equals and hashCode methods

    @Override
    protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        long start = System.currentTimeMillis();
        try {
            super.setPerformancePreferences(connectionTime, latency, bandwidth);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected void create(boolean stream) throws IOException {
        long start = System.currentTimeMillis();
        try {
            super.create(stream);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed(this.address = new InetSocketAddress(host, port));
            super.connect(host, port);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed(this.address = new InetSocketAddress(address, port));
            super.connect(address, port);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
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
    protected void bind(InetAddress host, int port) throws IOException {
        long start = System.currentTimeMillis();
        try {
            super.bind(host, port); // TODO: should we check connectivity enabled here as well ?
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected void listen(int backlog) throws IOException {
        long start = System.currentTimeMillis();
        try {
            super.listen(backlog);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        long start = System.currentTimeMillis();
        try {
            super.accept(s);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        long start = System.currentTimeMillis();
        checkConnectionAllowed();
        try {
            return new SnifferInputStream(this, super.getInputStream());
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        long start = System.currentTimeMillis();
        checkConnectionAllowed();
        try {
            return new SnifferOutputStream(this, super.getOutputStream());
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected int available() throws IOException {
        long start = System.currentTimeMillis();
        try {
            return super.available();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected void close() throws IOException {
        checkConnectionAllowed(1);
        long start = System.currentTimeMillis();
        try {
            super.close();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    // interface

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        long start = System.currentTimeMillis();
        try {
            super.setOption(optID, value);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        long start = System.currentTimeMillis();
        try {
            return super.getOption(optID);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    public int getPotentiallyBufferedInputBytes() {
        return potentiallyBufferedInputBytes;
    }

    public void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes) {
        this.potentiallyBufferedInputBytes = potentiallyBufferedInputBytes;
    }

    public int getPotentiallyBufferedOutputBytes() {
        return potentiallyBufferedOutputBytes;
    }

    public void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes) {
        this.potentiallyBufferedOutputBytes = potentiallyBufferedOutputBytes;
    }

    public long getLastReadThreadId() {
        return lastReadThreadId;
    }

    public void setLastReadThreadId(long lastReadThreadId) {
        this.lastReadThreadId = lastReadThreadId;
    }

    public long getLastWriteThreadId() {
        return lastWriteThreadId;
    }

    public void setLastWriteThreadId(long lastWriteThreadId) {
        this.lastWriteThreadId = lastWriteThreadId;
    }

}
