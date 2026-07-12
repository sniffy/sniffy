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
import java.io.InputStream;
import java.io.OutputStream;
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
    private static final IoOperationHook NOOP_IO_OPERATION_HOOK = new IoOperationHook() {
        @Override public void beforeWrite() {
        }
        @Override public void beforeRead() {
        }
        @Override public void afterPhysicalWrite() {
        }
        @Override public void afterPhysicalRead() {
        }
        @Override public void beforeFinalTrafficPublication() {
        }
    };

    private final int connectionId = Sniffy.CONNECTION_ID_SEQUENCE.getAndIncrement();
    private final Socket socket;
    private final IoOperationHook ioOperationHook;

    /* A physical operation and its accounting event share this lock, preserving wire order. */
    private final Object connectionReadLock = new Object();
    private final Object connectionWriteLock = new Object();
    private final Object bufferAccountingLock = new Object();
    private final Object endpointPolicyLock = new Object();
    private volatile InetSocketAddress physicalAddress;
    private volatile EffectiveEndpointPolicy effectiveEndpointPolicy =
            new EffectiveEndpointPolicy(null, null, 0L, false);

    // fields related to injecting latency fault
    private volatile int potentiallyBufferedInputBytes = 0;
    private volatile int potentiallyBufferedOutputBytes = 0;

    private volatile long lastReadThreadId;
    private volatile long lastWriteThreadId;

    private boolean firstChunk = true;
    private final ByteArrayOutputStream initialOutboundBytes = new ByteArrayOutputStream();

    protected SniffySocketChannel(SelectorProvider provider, SocketChannel delegate) throws SocketException {
        this(provider, delegate, NOOP_IO_OPERATION_HOOK);
    }

    SniffySocketChannel(SelectorProvider provider, SocketChannel delegate, IoOperationHook ioOperationHook)
            throws SocketException {
        super(provider, delegate);
        this.ioOperationHook = ioOperationHook;
        SocketAddress remoteAddress = delegate.socket().getRemoteSocketAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            physicalAddress = (InetSocketAddress) remoteAddress;
            effectiveEndpointPolicy = new EffectiveEndpointPolicy(physicalAddress, null, 0L, false);
        }
        this.socket = new SniffySocketChannelSocket(super.socket(), this, connectionId);
        LOG.trace("Created new SniffySocketChannel(" + provider + ", " + delegate + ") = " + this);
    }

    @Override
    public void setConnectionStatus(Integer connectionStatus) {
        synchronized (endpointPolicyLock) {
            EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            effectiveEndpointPolicy = current.withStatus(connectionStatus);
        }
    }

    @Override
    public void setConnectionStatus(InetSocketAddress endpoint, Integer connectionStatus) {
        synchronized (endpointPolicyLock) {
            EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            if (sameEndpoint(current.address, endpoint)) {
                effectiveEndpointPolicy = current.withStatus(connectionStatus);
            }
        }
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        try {
            SocketAddress remote = getRemoteAddress();
            if (remote instanceof InetSocketAddress) {
                physicalAddress = (InetSocketAddress) remote;
            }
        } catch (Exception e) {
            if (physicalAddress == null) throw ExceptionUtil.processException(e);
        }
        return physicalAddress;
    }

    private volatile boolean firstPacketSent;

    @Override
    public void setProxiedInetSocketAddress(InetSocketAddress proxiedAddress) {
        synchronized (endpointPolicyLock) {
            EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            effectiveEndpointPolicy = new EffectiveEndpointPolicy(
                    proxiedAddress, null, current.generation + 1L, true);
        }
    }

    @Override
    public void setProxiedInetSocketAddressAndStatus(InetSocketAddress proxiedAddress, Integer connectionStatus) {
        synchronized (endpointPolicyLock) {
            EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            effectiveEndpointPolicy = new EffectiveEndpointPolicy(
                    proxiedAddress, connectionStatus, current.generation + 1L, true);
        }
    }

    @Override
    public InetSocketAddress getProxiedInetSocketAddress() {
        EffectiveEndpointPolicy snapshot = effectiveEndpointPolicy;
        return snapshot.proxied ? snapshot.address : null;
    }

    @Override
    public void setFirstPacketSent(boolean firstPacketSent) {
        this.firstPacketSent = firstPacketSent;
    }

    @Override
    public boolean isFirstPacketSent() {
        return firstPacketSent;
    }

    private void sleepIfRequired(int bytesDown, EffectiveEndpointPolicy policy) throws ConnectException {
        int delayCycles = 0;
        synchronized (bufferAccountingLock) {
            lastReadThreadId = Thread.currentThread().getId();
            if (lastReadThreadId == lastWriteThreadId) {
                potentiallyBufferedOutputBytes = 0;
            }
            potentiallyBufferedInputBytes -= bytesDown;
            if (potentiallyBufferedInputBytes < 0) {
                delayCycles = 1 + (-1 * potentiallyBufferedInputBytes)
                        / SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
                potentiallyBufferedInputBytes = SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
            }
        }
        // Policy resolution and sleeping can re-enter instrumentation; never perform them under
        // the short cross-direction accounting critical section.
        if (delayCycles > 0) checkConnectionAllowed(policy, delayCycles);
    }

    private void sleepIfRequiredForWrite(int bytesUp, EffectiveEndpointPolicy policy) throws ConnectException {
        int delayCycles = 0;
        synchronized (bufferAccountingLock) {
            lastWriteThreadId = Thread.currentThread().getId();
            if (lastReadThreadId == lastWriteThreadId) {
                potentiallyBufferedInputBytes = 0;
            }
            potentiallyBufferedOutputBytes -= bytesUp;
            if (potentiallyBufferedOutputBytes < 0) {
                delayCycles = 1 + (-1 * potentiallyBufferedOutputBytes)
                        / SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
                potentiallyBufferedOutputBytes = SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
            }
        }
        if (delayCycles > 0) checkConnectionAllowed(policy, delayCycles);
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
            correlateFirstOutboundChunk(sent, traffic, off, len);
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
                correlateFirstOutboundChunk(sent, traffic, off, len);
            }
        }
    }

    private void correlateFirstOutboundChunk(boolean sent, byte[] traffic, int off, int len) {
        if (sent && firstChunk) {
            try {
                SniffySSLNetworkConnection sniffySSLNetworkConnection =
                        Sniffy.CLIENT_HELLO_CACHE.get(ByteBuffer.wrap(traffic, off, len));
                if (null != sniffySSLNetworkConnection) {
                    sniffySSLNetworkConnection.setSniffyNetworkConnection(this);
                }
            } finally {
                // A captured non-CONNECT outbound chunk consumes the single TLS correlation attempt,
                // including cache misses and callback failures.
                firstChunk = false;
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
        if (!SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled()) return;
        checkConnectionAllowed(policySnapshot(), numberOfSleepCycles);
    }

    public void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException {
        checkConnectionAllowed(inetSocketAddress, 1);
    }

    public void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException {
        if (!SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled()) return;
        checkConnectionAllowed(policySnapshot(inetSocketAddress), numberOfSleepCycles);
    }

    private void checkConnectionAllowed(EffectiveEndpointPolicy policy, int numberOfSleepCycles) throws ConnectException {

        if (!SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled()) return;

        if (null != policy.address && null != policy.status) {
            Integer connectionStatus = policy.status;
            if (connectionStatus < 0) {
                if (numberOfSleepCycles > 0 && -1 != connectionStatus) try {
                    sleepImpl(-1 * connectionStatus * numberOfSleepCycles);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                throw new ConnectException(String.format("Connection to %s refused by Sniffy", policy.address));
            } else if (numberOfSleepCycles > 0 && connectionStatus > 0) {
                try {
                    sleepImpl(connectionStatus * numberOfSleepCycles);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private EffectiveEndpointPolicy policySnapshot() {
        EffectiveEndpointPolicy current = effectiveEndpointPolicy;
        InetSocketAddress address = current.address;
        if (address == null) {
            address = getInetSocketAddress();
        }
        return resolvePolicy(current, address);
    }

    private EffectiveEndpointPolicy operationPolicySnapshot() {
        return SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled()
                ? policySnapshot() : effectiveEndpointPolicy;
    }

    private EffectiveEndpointPolicy policySnapshot(InetSocketAddress address) {
        return resolvePolicy(effectiveEndpointPolicy, address);
    }

    private EffectiveEndpointPolicy resolvePolicy(EffectiveEndpointPolicy observed, InetSocketAddress address) {
        if (address == null) return observed;
        boolean sameAddress = sameEndpoint(observed.address, address);
        if (sameAddress && observed.status != null && !ConnectionsRegistry.INSTANCE.isThreadLocal()) {
            return observed;
        }

        int resolved = ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(address, this);
        EffectiveEndpointPolicy resolvedPolicy = new EffectiveEndpointPolicy(
                address, resolved, observed.generation + 1L, sameAddress && observed.proxied);
        if (ConnectionsRegistry.INSTANCE.isThreadLocal()) {
            return resolvedPolicy;
        }
        synchronized (endpointPolicyLock) {
            EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            if (current == observed || (sameEndpoint(current.address, address) && current.status == null)) {
                effectiveEndpointPolicy = new EffectiveEndpointPolicy(
                        address, resolved, current.generation + 1L, current.proxied);
                return effectiveEndpointPolicy;
            }
            return current;
        }
    }

    private static boolean sameEndpoint(InetSocketAddress left, InetSocketAddress right) {
        if (left == right) return true;
        if (left == null || right == null || left.getPort() != right.getPort()) return false;
        if (left.getAddress() != null && right.getAddress() != null
                && left.getAddress().equals(right.getAddress())) return true;
        return left.getHostString().equalsIgnoreCase(right.getHostString());
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
                physicalAddress = (InetSocketAddress) remote;
            }
            return super.connect(remote);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ioOperationHook.beforeRead();
        synchronized (connectionReadLock) {
            // The physical read and immutable byte copy are one ordered event shared with streams.
            EffectiveEndpointPolicy policy = operationPolicySnapshot();
            checkConnectionAllowed(policy, 0);
            long start = System.currentTimeMillis();
            int bytesDown = 0;
            int position = dst.position();
            try {
                bytesDown = super.read(dst);
                if (bytesDown > 0) ioOperationHook.afterPhysicalRead();
                return bytesDown;
            } finally {
                if (bytesDown > 0) {
                    sleepIfRequired(bytesDown, policy);
                    logSocket(System.currentTimeMillis() - start, bytesDown, 0);
                    if (Sniffy.getEffectiveSpyConfiguration().isCaptureNetworkTraffic()) {
                        byte[] buff = copyBytes(dst, position, bytesDown);
                        logTraffic(false, Protocol.TCP, buff, 0, buff.length);
                    }
                } else {
                    logSocket(System.currentTimeMillis() - start, 0, 0);
                }
            }
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        ioOperationHook.beforeRead();
        synchronized (connectionReadLock) {
            EffectiveEndpointPolicy policy = operationPolicySnapshot();
            checkConnectionAllowed(policy, 0);
            long start = System.currentTimeMillis();
            long bytesDown = 0;
            int[] positions = new int[length];
            for (int i = 0; i < length; i++) {
                positions[i] = dsts[offset + i].position();
            }
            try {
                bytesDown = super.read(dsts, offset, length);
                if (bytesDown > 0) ioOperationHook.afterPhysicalRead();
                return bytesDown;
            } finally {
                if (bytesDown > 0) {
                    recordRead(bytesDown, start, policy);
                } else {
                    logSocket(System.currentTimeMillis() - start, 0, 0);
                }
                if (bytesDown > 0 && Sniffy.getEffectiveSpyConfiguration().isCaptureNetworkTraffic()) {
                    for (int i = 0; i < length; i++) {
                        int transferred = dsts[offset + i].position() - positions[i];
                        if (transferred > 0) {
                            byte[] buff = copyBytes(dsts[offset + i], positions[i], transferred);
                            logTraffic(false, Protocol.TCP, buff, 0, buff.length);
                        }
                    }
                }
            }
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        ioOperationHook.beforeWrite();
        synchronized (connectionWriteLock) {
            // Starting policy, physical write, accounting, and parser publication are one event.
            EffectiveEndpointPolicy policy = operationPolicySnapshot();
            checkConnectionAllowed(policy, 0);
            long start = System.currentTimeMillis();
            int written = 0;
            int position = src.position();
            try {
                written = super.write(src);
                if (written > 0) ioOperationHook.afterPhysicalWrite();
                return written;
            } finally {
                recordCompletedWrite(src, position, written, start, policy);
            }
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        ioOperationHook.beforeWrite();
        synchronized (connectionWriteLock) {
            EffectiveEndpointPolicy policy = operationPolicySnapshot();
            checkConnectionAllowed(policy, 0);
            long start = System.currentTimeMillis();
            long bytesUp = 0;
            int[] positions = new int[length];
            for (int i = 0; i < length; i++) {
                positions[i] = srcs[offset + i].position();
            }
            try {
                bytesUp = super.write(srcs, offset, length);
                if (bytesUp > 0) ioOperationHook.afterPhysicalWrite();
                return bytesUp;
            } finally {
                if (bytesUp > 0) {
                    recordWrite(bytesUp, start, policy);
                } else {
                    logSocket(System.currentTimeMillis() - start, 0, 0);
                }
                boolean capture = Sniffy.getEffectiveSpyConfiguration().isCaptureNetworkTraffic();
                if (bytesUp > 0) {
                    for (int i = 0; i < length; i++) {
                        int transferred = srcs[offset + i].position() - positions[i];
                        if (transferred > 0) {
                            processOutboundBytes(copyBytes(srcs[offset + i], positions[i], transferred), capture);
                        }
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

    void processOutboundBytes(byte[] bytes, boolean captureNetworkTraffic) {
        synchronized (connectionWriteLock) {
            processOutboundBytesLocked(bytes, captureNetworkTraffic);
        }
    }

    private void processOutboundBytesLocked(byte[] bytes, boolean captureNetworkTraffic) {
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
            int handshakeLength = null == getProxiedInetSocketAddress() ? 0 : httpHeaderLength(candidate);
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

    int pendingInitialOutboundByteCount() {
        synchronized (connectionWriteLock) {
            return initialOutboundBytes.size();
        }
    }

    Integer connectionStatus() {
        return effectiveEndpointPolicy.status;
    }

    EffectiveEndpointPolicy effectiveEndpointPolicy() {
        return effectiveEndpointPolicy;
    }

    private void finishOutboundInspection() {
        synchronized (connectionWriteLock) {
            finishOutboundInspectionLocked();
        }
    }

    private void finishOutboundInspectionLocked() {
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
                ioOperationHook.beforeFinalTrafficPublication();
                logTraffic(true, Protocol.TCP, pending, 0, pending.length, false);
            }
        }
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        Throwable failure = null;
        try {
            super.shutdownOutput();
        } catch (Throwable e) {
            failure = e;
        }
        try {
            finishOutboundInspection();
        } catch (Throwable e) {
            failure = combine(failure, e);
        }
        if (failure != null) rethrow(failure);
        return this;
    }

    @Override
    public void implCloseSelectableChannel() throws IOException {
        Throwable failure = null;
        // Physical close runs first so it can release a thread blocked while holding an I/O lock.
        try {
            super.implCloseSelectableChannel();
        } catch (Throwable e) {
            failure = e;
        }
        try {
            finishOutboundInspection();
        } catch (Throwable e) {
            failure = combine(failure, e);
        }
        try {
            ConnectionsRegistry.INSTANCE.unregisterNetworkConnection(this);
        } catch (Throwable e) {
            failure = combine(failure, e);
        }
        if (failure != null) rethrow(failure);
    }

    private void recordRead(long bytesDown, long start, EffectiveEndpointPolicy policy) throws ConnectException {
        long remaining = bytesDown;
        boolean first = true;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
            sleepIfRequired(chunk, policy);
            logSocket(first ? System.currentTimeMillis() - start : 0, chunk, 0);
            first = false;
            remaining -= chunk;
        }
    }

    private void recordWrite(long bytesUp, long start, EffectiveEndpointPolicy policy) throws ConnectException {
        long remaining = bytesUp;
        boolean first = true;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
            sleepIfRequiredForWrite(chunk, policy);
            logSocket(first ? System.currentTimeMillis() - start : 0, 0, chunk);
            first = false;
            remaining -= chunk;
        }
    }

    private void recordCompletedWrite(ByteBuffer src, int position, int written, long start,
                                      EffectiveEndpointPolicy policy) throws ConnectException {
        if (written > 0) {
            sleepIfRequiredForWrite(written, policy);
            logSocket(System.currentTimeMillis() - start, 0, written);
            boolean capture = Sniffy.getEffectiveSpyConfiguration().isCaptureNetworkTraffic();
            if (capture || !isFirstPacketSent()) {
                processOutboundBytesLocked(copyBytes(src, position, written), capture);
            }
        } else {
            logSocket(System.currentTimeMillis() - start, 0, 0);
        }
    }

    int read(InputStream delegate) throws IOException {
        ioOperationHook.beforeRead();
        synchronized (connectionReadLock) {
            EffectiveEndpointPolicy policy = operationPolicySnapshot();
            checkConnectionAllowed(policy, 0);
            long start = System.currentTimeMillis();
            int value = -1;
            try {
                value = delegate.read();
                if (value >= 0) ioOperationHook.afterPhysicalRead();
                return value;
            } finally {
                int accountedBytes = value < 0 ? 0 : 1;
                if (accountedBytes > 0) {
                    sleepIfRequired(accountedBytes, policy);
                    logTraffic(false, Protocol.TCP, new byte[]{(byte) value}, 0, 1);
                }
                logSocket(System.currentTimeMillis() - start, accountedBytes, 0);
            }
        }
    }

    int read(InputStream delegate, byte[] bytes) throws IOException {
        return read(delegate, bytes, 0, bytes.length, false);
    }

    int read(InputStream delegate, byte[] bytes, int offset, int length) throws IOException {
        return read(delegate, bytes, offset, length, true);
    }

    private int read(InputStream delegate, byte[] bytes, int offset, int length, boolean ranged) throws IOException {
        ioOperationHook.beforeRead();
        synchronized (connectionReadLock) {
            EffectiveEndpointPolicy policy = operationPolicySnapshot();
            checkConnectionAllowed(policy, 0);
            long start = System.currentTimeMillis();
            int read = 0;
            try {
                read = ranged ? delegate.read(bytes, offset, length) : delegate.read(bytes);
                if (read > 0) ioOperationHook.afterPhysicalRead();
                return read;
            } finally {
                int accountedBytes = Math.max(read, 0);
                if (accountedBytes > 0) {
                    sleepIfRequired(accountedBytes, policy);
                    logTraffic(false, Protocol.TCP, bytes, offset, accountedBytes);
                }
                logSocket(System.currentTimeMillis() - start, accountedBytes, 0);
            }
        }
    }

    void write(OutputStream delegate, int value) throws IOException {
        ioOperationHook.beforeWrite();
        synchronized (connectionWriteLock) {
            EffectiveEndpointPolicy policy = operationPolicySnapshot();
            checkConnectionAllowed(policy, 0);
            long start = System.currentTimeMillis();
            boolean written = false;
            try {
                delegate.write(value);
                written = true;
                ioOperationHook.afterPhysicalWrite();
            } finally {
                int accountedBytes = written ? 1 : 0;
                if (written) {
                    recordWrite(1, start, policy);
                    processOutboundBytesLocked(new byte[]{(byte) value},
                            Sniffy.getEffectiveSpyConfiguration().isCaptureNetworkTraffic());
                } else {
                    logSocket(System.currentTimeMillis() - start, 0, accountedBytes);
                }
            }
        }
    }

    void write(OutputStream delegate, byte[] bytes) throws IOException {
        write(delegate, bytes, 0, bytes.length, false);
    }

    void write(OutputStream delegate, byte[] bytes, int offset, int length) throws IOException {
        write(delegate, bytes, offset, length, true);
    }

    private void write(OutputStream delegate, byte[] bytes, int offset, int length, boolean ranged) throws IOException {
        ioOperationHook.beforeWrite();
        synchronized (connectionWriteLock) {
            EffectiveEndpointPolicy policy = operationPolicySnapshot();
            checkConnectionAllowed(policy, 0);
            long start = System.currentTimeMillis();
            boolean written = false;
            try {
                if (ranged) delegate.write(bytes, offset, length); else delegate.write(bytes);
                written = true;
                ioOperationHook.afterPhysicalWrite();
            } finally {
                if (written) {
                    recordWrite(length, start, policy);
                    byte[] copy = new byte[length];
                    System.arraycopy(bytes, offset, copy, 0, length);
                    processOutboundBytesLocked(copy,
                            Sniffy.getEffectiveSpyConfiguration().isCaptureNetworkTraffic());
                } else {
                    logSocket(System.currentTimeMillis() - start, 0, 0);
                }
            }
        }
    }

    void closeInputStream(InputStream delegate) throws IOException {
        close();
    }

    void closeOutputStream(OutputStream delegate) throws IOException {
        close();
    }

    private static Throwable combine(Throwable primary, Throwable secondary) {
        if (primary == null) return secondary;
        if (primary != secondary) primary.addSuppressed(secondary);
        return primary;
    }

    private static void rethrow(Throwable failure) throws IOException {
        if (failure instanceof IOException) throw (IOException) failure;
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        if (failure instanceof Error) throw (Error) failure;
        throw new IOException(failure);
    }

    static final class EffectiveEndpointPolicy {
        final InetSocketAddress address;
        final Integer status;
        final long generation;
        final boolean proxied;

        EffectiveEndpointPolicy(InetSocketAddress address, Integer status, long generation, boolean proxied) {
            this.address = address;
            this.status = status;
            this.generation = generation;
            this.proxied = proxied;
        }

        EffectiveEndpointPolicy withStatus(Integer newStatus) {
            return new EffectiveEndpointPolicy(address, newStatus, generation + 1L, proxied);
        }
    }

    interface IoOperationHook {
        void beforeWrite() throws IOException;
        void beforeRead() throws IOException;
        void afterPhysicalWrite() throws IOException;
        void afterPhysicalRead() throws IOException;
        void beforeFinalTrafficPublication();
    }

    //

    @Override
    public int getPotentiallyBufferedInputBytes() {
        synchronized (bufferAccountingLock) {
            return potentiallyBufferedInputBytes;
        }
    }

    @Override
    public void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes) {
        synchronized (bufferAccountingLock) {
            this.potentiallyBufferedInputBytes = potentiallyBufferedInputBytes;
        }
    }

    @Override
    public int getPotentiallyBufferedOutputBytes() {
        synchronized (bufferAccountingLock) {
            return potentiallyBufferedOutputBytes;
        }
    }

    @Override
    public void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes) {
        synchronized (bufferAccountingLock) {
            this.potentiallyBufferedOutputBytes = potentiallyBufferedOutputBytes;
        }
    }

    @Override
    public long getLastReadThreadId() {
        synchronized (bufferAccountingLock) {
            return lastReadThreadId;
        }
    }

    @Override
    public void setLastReadThreadId(long lastReadThreadId) {
        synchronized (bufferAccountingLock) {
            this.lastReadThreadId = lastReadThreadId;
        }
    }

    @Override
    public long getLastWriteThreadId() {
        synchronized (bufferAccountingLock) {
            return lastWriteThreadId;
        }
    }

    @Override
    public void setLastWriteThreadId(long lastWriteThreadId) {
        synchronized (bufferAccountingLock) {
            this.lastWriteThreadId = lastWriteThreadId;
        }
    }

}
