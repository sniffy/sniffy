package io.sniffy.nio.compat;

import io.sniffy.Sniffy;
import io.sniffy.SpyConfiguration;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.socket.SniffySSLNetworkConnection;
import io.sniffy.util.ExceptionUtil;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// TODO: this functionality is available in java 1.7+ only - make sure it is safe
/**
 * @since 3.1.7
 */
public class SniffyAsynchronousSocketChannel extends AsynchronousSocketChannel implements SniffyNetworkConnection {

    private static final Polyglog LOG = PolyglogFactory.log(SniffyAsynchronousSocketChannel.class);

    private final AsynchronousSocketChannel delegate;

    private final int id = Sniffy.CONNECTION_ID_SEQUENCE.getAndIncrement();

    // fields related to injecting latency fault
    private volatile int potentiallyBufferedInputBytes = 0;
    private volatile int potentiallyBufferedOutputBytes = 0;

    private volatile long lastReadThreadId;
    private volatile long lastWriteThreadId;

    private volatile Integer connectionStatus;

    private boolean firstChunk = true;

    public SniffyAsynchronousSocketChannel(AsynchronousChannelProvider provider, AsynchronousSocketChannel delegate) {
        super(provider);
        this.delegate = delegate;
    }

    @Override
    public void setConnectionStatus(Integer connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        try {
            return (InetSocketAddress) delegate.getRemoteAddress();
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }


    private boolean firstPacketSent;
    private InetSocketAddress proxiedAddress;

    public void setProxiedInetSocketAddress(InetSocketAddress proxiedAddress) {
        this.proxiedAddress = proxiedAddress;
    }

    public InetSocketAddress getProxiedInetSocketAddress() {
        return proxiedAddress;
    }

    public void setFirstPacketSent(boolean firstPacketSent) {
        this.firstPacketSent = firstPacketSent;
    }

    public boolean isFirstPacketSent() {
        return firstPacketSent;
    }


    /**
     * Adds a delay as defined for current {@link SniffyAsynchronousSocketChannel} in {@link ConnectionsRegistry#discoveredDataSources}
     * <p>
     * Delay is added for each <b>N</b> bytes received where <b>N</b> is the value of {@link SocketOptions#SO_RCVBUF}
     * <p>
     * If application reads <b>M</b> bytes where (k-1) * N &lt; M  &lt; k * N exactly <b>k</b> delays will be added
     *
     * @param bytesDown number of bytes received from socket
     * @throws ConnectException on underlying socket exception
     */
    @SuppressWarnings("JavadocReference")
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

    /**
     * Adds a delay as defined for current {@link SniffyAsynchronousSocketChannel} in {@link ConnectionsRegistry#discoveredDataSources}
     * <p>
     * Delay is added for each <b>N</b> bytes sent where <b>N</b> is the value of {@link SocketOptions#SO_SNDBUF}
     * <p>
     * If application writes <b>M</b> bytes where (k-1) * N &lt; M  &lt; k * N exactly <b>k</b> delays will be added
     *
     * @param bytesUp number of bytes sent to socket
     * @throws ConnectException on underlying socket exception
     */
    @SuppressWarnings("JavadocReference")
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

    private static void sleepImpl(int millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @Deprecated
    public void logSocket(long millis) {
        logSocket(millis, 0, 0);
    }

    @Deprecated
    public void logSocket(long millis, int bytesDown, int bytesUp) {

        if (!SniffyConfiguration.INSTANCE.getSocketCaptureEnabled()) return;

        Sniffy.SniffyMode sniffyMode = Sniffy.getSniffyMode();
        if (sniffyMode.isEnabled() && null != getInetSocketAddress() && (millis > 0 || bytesDown > 0 || bytesUp > 0)) {
            Sniffy.logSocket(id, getInetSocketAddress(), millis, bytesDown, bytesUp, sniffyMode.isCaptureStackTraces());
        }
    }

    public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            LOG.trace("SniffyAsynchronousSocketChannel.logTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + id);
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
            LOG.trace("SniffyAsynchronousSocketChannel.logDecryptedTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + id);
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
        checkConnectionAllowed(getInetSocketAddress(), numberOfSleepCycles);
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

    public static AsynchronousSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        return AsynchronousSocketChannel.open(group);
    }

    public static AsynchronousSocketChannel open() throws IOException {
        return AsynchronousSocketChannel.open();
    }

    @Override
    public AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
        return delegate.bind(local);
    }

    @Override
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        return delegate.setOption(name, value);
    }

    @Override
    public AsynchronousSocketChannel shutdownInput() throws IOException {
        return delegate.shutdownInput();
    }

    @Override
    public AsynchronousSocketChannel shutdownOutput() throws IOException {
        return delegate.shutdownOutput();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return delegate.getRemoteAddress();
    }

    @Override
    public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        delegate.connect(remote, attachment, handler);
    }

    @Override
    public Future<Void> connect(SocketAddress remote) {
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed(1);
            return delegate.connect(remote);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        delegate.read(dst, timeout, unit, attachment, handler);
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {

        final long start = System.currentTimeMillis();

        final Future<Integer> integerFuture = delegate.read(dst);

        return new Future<Integer>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return integerFuture.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return integerFuture.isCancelled();
            }

            @Override
            public boolean isDone() {
                return integerFuture.isDone();
            }

            @Override
            public Integer get() throws InterruptedException, ExecutionException {
                Integer bytesDown = integerFuture.get();
                try {
                    checkConnectionAllowed(0);
                    sleepIfRequired(bytesDown);
                } catch (ConnectException e) {
                    throw new ExecutionException(new AsynchronousCloseException()); // TODO: this is all wrong
                }
                logSocket(System.currentTimeMillis() - start, bytesDown, 0);
                return bytesDown;
            }

            @Override
            public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                Integer bytesDown = integerFuture.get(timeout, unit);
                try {
                    checkConnectionAllowed(0);
                    sleepIfRequired(bytesDown);
                } catch (ConnectException e) {
                    throw new ExecutionException(new AsynchronousCloseException()); // TODO: this is all wrong
                }
                logSocket(System.currentTimeMillis() - start, bytesDown, 0);
                return bytesDown;
            }

        };

    }

    @Override
    public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        delegate.read(dsts, offset, length, timeout, unit, attachment, handler);
    }

    @Override
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        delegate.write(src, timeout, unit, attachment, handler);
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {

        final long start = System.currentTimeMillis();

        final Future<Integer> integerFuture = delegate.write(src);

        return new Future<Integer>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return integerFuture.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return integerFuture.isCancelled();
            }

            @Override
            public boolean isDone() {
                return integerFuture.isDone();
            }

            @Override
            public Integer get() throws InterruptedException, ExecutionException {
                Integer bytesUp = integerFuture.get();
                try {
                    checkConnectionAllowed(0);
                    sleepIfRequiredForWrite(bytesUp);
                } catch (ConnectException e) {
                    throw new ExecutionException(new AsynchronousCloseException()); // TODO: this is all wrong
                }
                logSocket(System.currentTimeMillis() - start, 0, bytesUp);
                return bytesUp;
            }

            @Override
            public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                Integer bytesUp = integerFuture.get(timeout, unit);
                try {
                    checkConnectionAllowed(0);
                    sleepIfRequiredForWrite(bytesUp);
                } catch (ConnectException e) {
                    throw new ExecutionException(new AsynchronousCloseException()); // TODO: this is all wrong
                }
                logSocket(System.currentTimeMillis() - start, 0, bytesUp);
                return bytesUp;
            }

        };
    }

    @Override
    public <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        delegate.write(srcs, offset, length, timeout, unit, attachment, handler);
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return delegate.getLocalAddress();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return delegate.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return delegate.supportedOptions();
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
