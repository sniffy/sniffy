package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.SpyConfiguration;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.*;
import io.sniffy.util.ExceptionUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * @since 3.1.7
 */
public class SniffySocketChannel extends SniffySocketChannelAdapter implements SniffyNetworkConnection {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySocketChannel.class);
    private static final int INITIAL_PACKET_CAPTURE_LIMIT = 8192;

    private final int connectionId = Sniffy.CONNECTION_ID_SEQUENCE.getAndIncrement();
    private final Socket socket;

    private volatile Integer connectionStatus;

    // fields related to injecting latency fault
    private volatile int potentiallyBufferedInputBytes = 0;
    private volatile int potentiallyBufferedOutputBytes = 0;

    private volatile long lastReadThreadId;
    private volatile long lastWriteThreadId;

    private boolean firstChunk = true;
    private final ByteArrayOutputStream initialOutboundBytes = new ByteArrayOutputStream();

    protected SniffySocketChannel(SelectorProvider provider, SocketChannel delegate) throws SocketException {
        super(provider, delegate);
        this.socket = new SniffySocketChannelSocket(super.socket(), this, connectionId);
        LOG.trace("Created new SniffySocketChannel(" + provider + ", " + delegate + ") = " + this);
    }

    @Override
    public void setConnectionStatus(Integer connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        try {
            return (InetSocketAddress) getRemoteAddress();
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
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

    private void sleepIfRequired(int bytesDown) throws ConnectException {

        lastReadThreadId = Thread.currentThread().getId();

        if (lastReadThreadId == lastWriteThreadId) {
            potentiallyBufferedOutputBytes = 0;
        }

        int potentiallyBufferedInputBytes = this.potentiallyBufferedInputBytes -= bytesDown;

        if (potentiallyBufferedInputBytes < 0) {
            int estimatedNumberOfTcpPackets = 1 + (-1 * potentiallyBufferedInputBytes) / SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
            checkConnectionAllowed(estimatedNumberOfTcpPackets);
            this.potentiallyBufferedInputBytes = SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
        }

    }

    private void sleepIfRequiredForWrite(int bytesUp) throws ConnectException {

        lastWriteThreadId = Thread.currentThread().getId();

        if (lastReadThreadId == lastWriteThreadId) {
            potentiallyBufferedInputBytes = 0;
        }

        int potentiallyBufferedOutputBytes = this.potentiallyBufferedOutputBytes -= bytesUp;

        if (potentiallyBufferedOutputBytes < 0) {
            int estimatedNumberOfTcpPackets = 1 + (-1 * potentiallyBufferedOutputBytes) / SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
            checkConnectionAllowed(estimatedNumberOfTcpPackets);
            this.potentiallyBufferedOutputBytes = SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
        }

    }

    @Deprecated
    @Override
    public void logSocket(long millis) {
        logSocket(millis, 0, 0);
    }

    @Deprecated
    @Override
    public void logSocket(long millis, int bytesDown, int bytesUp) {

        if (!SniffyConfiguration.INSTANCE.getSocketCaptureEnabled()) return;

        if (null != getInetSocketAddress() && (millis > 0 || bytesDown > 0 || bytesUp > 0)) {
            Sniffy.SniffyMode sniffyMode = Sniffy.getSniffyMode();
            if (sniffyMode.isEnabled()) {
                Sniffy.logSocket(connectionId, getInetSocketAddress(), millis, bytesDown, bytesUp, sniffyMode.isCaptureStackTraces()); // TODO: stack trace here should be calculated till another package
            }
        }
    }

    public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            LOG.trace("SniffySocketChannel.logTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + connectionId);
            Sniffy.logTraffic(
                    connectionId, getInetSocketAddress(),
                    sent, protocol,
                    traffic, off, len,
                    effectiveSpyConfiguration.isCaptureStackTraces()
            );
            if (sent && firstChunk) {
                SniffySSLNetworkConnection sniffySSLNetworkConnection = Sniffy.CLIENT_HELLO_CACHE.get(ByteBuffer.wrap(traffic, off, len));
                if (null != sniffySSLNetworkConnection) {
                    sniffySSLNetworkConnection.setSniffyNetworkConnection(this);
                }
                firstChunk = false;
            }
        }
    }

    public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len, boolean isConnectPacket) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            LOG.trace("SniffySocketChannel.logTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + connectionId);
            Sniffy.logTraffic(
                    connectionId, getInetSocketAddress(),
                    sent, protocol,
                    traffic, off, len,
                    effectiveSpyConfiguration.isCaptureStackTraces()
            );
            if (!isConnectPacket) {
                if (sent && firstChunk) {
                    SniffySSLNetworkConnection sniffySSLNetworkConnection = Sniffy.CLIENT_HELLO_CACHE.get(ByteBuffer.wrap(traffic, off, len));
                    if (null != sniffySSLNetworkConnection) {
                        sniffySSLNetworkConnection.setSniffyNetworkConnection(this);
                    }
                    firstChunk = false;
                }
            }
        }
    }

    public void logDecryptedTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            LOG.trace("SniffySocketChannel.logDecryptedTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + connectionId);
            Sniffy.logDecryptedTraffic(
                    connectionId, null == getProxiedInetSocketAddress() ? getInetSocketAddress() : getProxiedInetSocketAddress(),
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
        checkConnectionAllowed(null == proxiedAddress ? getInetSocketAddress() : proxiedAddress, numberOfSleepCycles);
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

    private static void sleepImpl(int millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        long start = System.currentTimeMillis();
        try {
            if (remote instanceof InetSocketAddress) {
                checkConnectionAllowed((InetSocketAddress) remote, 1);
            }
            return super.connect(remote);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        // TODO: honor SpyConfiguration.isBufferIncomingTraffic() and SniffyConfiguration.INSTANCE.getIncomingTrafficBufferSize() settings
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        int position = dst.position();
        try {
            return bytesDown = super.read(dst);
        } finally {
            if (bytesDown > 0) {
                sleepIfRequired(bytesDown);
                logSocket(System.currentTimeMillis() - start, bytesDown, 0);
                SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
                if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
                    byte[] buff = copyBytes(dst, position, bytesDown);
                    logTraffic(false, Protocol.TCP, buff, 0, buff.length);
                }
            } else {
                logSocket(System.currentTimeMillis() - start, 0, 0);
            }
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        // TODO: honor SpyConfiguration.isBufferIncomingTraffic() and SniffyConfiguration.INSTANCE.getIncomingTrafficBufferSize() settings
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        long bytesDown = 0;

        int[] positions = new int[length];

        for (int i = 0; i < length; i++) {
            positions[i] = dsts[offset + i].position();
        }

        try {
            bytesDown = super.read(dsts, offset, length);
            return bytesDown;
        } finally {
            if (bytesDown > 0) {
                recordRead(bytesDown, start);
            } else {
                logSocket(System.currentTimeMillis() - start, 0, 0);
            }

            if (bytesDown > 0 && Sniffy.getEffectiveSpyConfiguration().isCaptureNetworkTraffic()) {
                for (int i = 0; i < length; i++) {
                    int newPosition = dsts[offset + i].position();
                    int transferred = newPosition - positions[i];
                    if (transferred > 0) {
                        byte[] buff = copyBytes(dsts[offset + i], positions[i], transferred);
                        logTraffic(false, Protocol.TCP, buff, 0, buff.length);
                    }
                }
            }
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int length = 0;

        int position = src.position();

        try {
            length = super.write(src);
            return length;
        } finally {
            if (length > 0) {
                sleepIfRequiredForWrite(length);
                logSocket(System.currentTimeMillis() - start, 0, length);
            } else {
                logSocket(System.currentTimeMillis() - start, 0, 0);
            }
            SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
            if (length > 0 && (effectiveSpyConfiguration.isCaptureNetworkTraffic() || !isFirstPacketSent())) {
                byte[] buff = copyBytes(src, position, length);
                processOutboundBytes(buff, effectiveSpyConfiguration.isCaptureNetworkTraffic());
            }
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        long bytesUp = 0;

        int[] positions = new int[length];
        for (int i = 0; i < length; i++) {
            positions[i] = srcs[offset + i].position();
        }

        try {
            bytesUp = super.write(srcs, offset, length);
            return bytesUp;
        } finally {
            if (bytesUp > 0) {
                recordWrite(bytesUp, start);
            } else {
                logSocket(System.currentTimeMillis() - start, 0, 0);
            }
            SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();

            if (bytesUp > 0) {
                for (int i = 0; i < length; i++) {
                    int transferred = srcs[offset + i].position() - positions[i];
                    if (transferred > 0) {
                        byte[] buff = copyBytes(srcs[offset + i], positions[i], transferred);
                        processOutboundBytes(buff, effectiveSpyConfiguration.isCaptureNetworkTraffic());
                    }
                }

            }
        }
    }

    @Override
    public Socket socket() {
        return socket;
    }

    static byte[] copyBytes(ByteBuffer buffer, int position, int length) {
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.limit(position + length);
        duplicate.position(position);
        byte[] bytes = new byte[length];
        duplicate.get(bytes);
        return bytes;
    }

    static byte[] copyTransferredBytes(ByteBuffer[] buffers, int offset, int length,
                                       int[] initialPositions, long transferredBytes) {
        int capturedLength = (int) Math.min(transferredBytes, INITIAL_PACKET_CAPTURE_LIMIT);
        byte[] bytes = new byte[capturedLength];
        int destinationOffset = 0;
        for (int i = 0; i < length && destinationOffset < capturedLength; i++) {
            ByteBuffer buffer = buffers[offset + i];
            int transferred = Math.min(buffer.position() - initialPositions[i], capturedLength - destinationOffset);
            if (transferred > 0) {
                ByteBuffer duplicate = buffer.duplicate();
                duplicate.limit(initialPositions[i] + transferred);
                duplicate.position(initialPositions[i]);
                duplicate.get(bytes, destinationOffset, transferred);
                destinationOffset += transferred;
            }
        }
        if (destinationOffset == capturedLength) {
            return bytes;
        }
        byte[] exactBytes = new byte[destinationOffset];
        System.arraycopy(bytes, 0, exactBytes, 0, destinationOffset);
        return exactBytes;
    }

    synchronized void processOutboundBytes(byte[] bytes, boolean captureNetworkTraffic) {
        if (bytes.length == 0) return;

        if (isFirstPacketSent()) {
            if (captureNetworkTraffic) {
                logTraffic(true, Protocol.TCP, bytes, 0, bytes.length, false);
            }
            return;
        }

        if (!Boolean.TRUE.equals(SniffyConfiguration.INSTANCE.getInterceptProxyConnections())) {
            setFirstPacketSent(true);
            if (captureNetworkTraffic) {
                logTraffic(true, Protocol.TCP, bytes, 0, bytes.length, false);
            }
            return;
        }

        int available = INITIAL_PACKET_CAPTURE_LIMIT - initialOutboundBytes.size();
        int appended = Math.min(available, bytes.length);
        initialOutboundBytes.write(bytes, 0, appended);
        byte[] candidate = initialOutboundBytes.toByteArray();

        if (!isProxyDecisionComplete(candidate) && appended == bytes.length) {
            return;
        }

        try {
            new SniffyPacketAnalyzer(this).analyze(candidate, 0, candidate.length);
        } catch (Exception e) {
            LOG.error(e);
        } finally {
            setFirstPacketSent(true);
        }

        if (captureNetworkTraffic) {
            int handshakeLength = null == proxiedAddress ? 0 : httpHeaderLength(candidate);
            if (handshakeLength > 0) {
                logTraffic(true, Protocol.TCP, candidate, 0, handshakeLength, true);
            }
            if (handshakeLength < candidate.length) {
                logTraffic(true, Protocol.TCP, candidate, handshakeLength, candidate.length - handshakeLength, false);
            }
            if (appended < bytes.length) {
                logTraffic(true, Protocol.TCP, bytes, appended, bytes.length - appended, false);
            }
        }
        initialOutboundBytes.reset();
    }

    private static boolean isProxyDecisionComplete(byte[] candidate) {
        byte[] connectPrefix = new byte[]{'C', 'O', 'N', 'N', 'E', 'C', 'T', ' '};
        int prefixLength = Math.min(candidate.length, connectPrefix.length);
        for (int i = 0; i < prefixLength; i++) {
            if (candidate[i] != connectPrefix[i]) return true;
        }
        if (candidate.length < connectPrefix.length) return false;
        if (httpHeaderLength(candidate) > 0) return true;
        return candidate.length >= INITIAL_PACKET_CAPTURE_LIMIT;
    }

    private static int httpHeaderLength(byte[] bytes) {
        for (int i = 1; i < bytes.length; i++) {
            if ('\n' == bytes[i] && '\n' == bytes[i - 1]) return i + 1;
            if (i >= 3 && '\r' == bytes[i - 3] && '\n' == bytes[i - 2] &&
                    '\r' == bytes[i - 1] && '\n' == bytes[i]) {
                return i + 1;
            }
        }
        return 0;
    }

    synchronized int pendingInitialOutboundByteCount() {
        return initialOutboundBytes.size();
    }

    synchronized Integer connectionStatus() {
        return connectionStatus;
    }

    private synchronized void finishOutboundInspection() {
        if (!isFirstPacketSent() && initialOutboundBytes.size() > 0) {
            byte[] pending = initialOutboundBytes.toByteArray();
            try {
                new SniffyPacketAnalyzer(this).analyze(pending, 0, pending.length);
            } catch (Exception e) {
                LOG.error(e);
            } finally {
                setFirstPacketSent(true);
                initialOutboundBytes.reset();
            }
            if (Sniffy.getEffectiveSpyConfiguration().isCaptureNetworkTraffic()) {
                logTraffic(true, Protocol.TCP, pending, 0, pending.length, false);
            }
        }
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        try {
            return super.shutdownOutput();
        } finally {
            finishOutboundInspection();
        }
    }

    @Override
    public void implCloseSelectableChannel() throws IOException {
        finishOutboundInspection();
        super.implCloseSelectableChannel();
    }

    private void recordRead(long bytesDown, long start) throws ConnectException {
        long remaining = bytesDown;
        boolean first = true;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
            sleepIfRequired(chunk);
            logSocket(first ? System.currentTimeMillis() - start : 0, chunk, 0);
            first = false;
            remaining -= chunk;
        }
    }

    private void recordWrite(long bytesUp, long start) throws ConnectException {
        long remaining = bytesUp;
        boolean first = true;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
            sleepIfRequiredForWrite(chunk);
            logSocket(first ? System.currentTimeMillis() - start : 0, 0, chunk);
            first = false;
            remaining -= chunk;
        }
    }

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
