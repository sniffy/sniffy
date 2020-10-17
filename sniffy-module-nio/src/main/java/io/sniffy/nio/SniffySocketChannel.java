package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.SniffySocket;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionFieldCopier;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

public class SniffySocketChannel extends SocketChannel implements SniffySocket, SelChImpl {

    // TODO: evaluate list of fields in various Java versions

    private final static ReflectionFieldCopier keysCopier =
            new ReflectionFieldCopier(AbstractSelectableChannel.class, "keys");
    private final static ReflectionFieldCopier keyCountCopier =
            new ReflectionFieldCopier(AbstractSelectableChannel.class, "keyCount");
    private final static ReflectionFieldCopier keyLockCopier =
            new ReflectionFieldCopier(AbstractSelectableChannel.class, "keyLock");
    private final static ReflectionFieldCopier regLockCopier =
            new ReflectionFieldCopier(AbstractSelectableChannel.class, "regLock");
    private final static ReflectionFieldCopier nonBlockingCopier =
            new ReflectionFieldCopier(AbstractSelectableChannel.class, "nonBlocking");
    private final static ReflectionFieldCopier blockingCopier =
            new ReflectionFieldCopier(AbstractSelectableChannel.class, "blocking");

    private final static ReflectionFieldCopier closeLockCopier =
            new ReflectionFieldCopier(AbstractInterruptibleChannel.class, "closeLock");
    private final static ReflectionFieldCopier closedCopier =
            new ReflectionFieldCopier(AbstractInterruptibleChannel.class, "closed");

    private static volatile ReflectionFieldCopier[] reflectionFieldCopiers;

    private final SocketChannel delegate;
    private final SelChImpl selChImplDelegate;

    private volatile Integer connectionStatus;

    private final static AtomicInteger counter = new AtomicInteger();

    private final int id = counter.getAndIncrement();

    protected static volatile Integer defaultReceiveBufferSize;
    protected int receiveBufferSize = -1;

    protected static volatile Integer defaultSendBufferSize;
    protected int sendBufferSize = -1;

    protected volatile int potentiallyBufferedInputBytes = 0;
    protected volatile int potentiallyBufferedOutputBytes = 0;

    protected volatile long lastReadThreadId;
    protected volatile long lastWriteThreadId;

    protected SniffySocketChannel(SelectorProvider provider, SocketChannel delegate) {
        super(provider);
        this.delegate = delegate;
        this.selChImplDelegate = (SelChImpl) delegate;
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


    // TODO: use heuristics based on popular defaults for TCP window sizes
    private void sleepIfRequired(int bytesDown) throws ConnectException {

        lastReadThreadId = Thread.currentThread().getId();

        if (lastReadThreadId == lastWriteThreadId) {
            potentiallyBufferedOutputBytes = 0;
        }

        if (0 == receiveBufferSize) {
            checkConnectionAllowed(1);
        } else {

            int potentiallyBufferedInputBytes = this.potentiallyBufferedInputBytes -= bytesDown;

            if (potentiallyBufferedInputBytes < 0) {
                int estimatedNumberOfTcpPackets = 1 + (-1 * potentiallyBufferedInputBytes) / receiveBufferSize;
                checkConnectionAllowed(estimatedNumberOfTcpPackets);
                this.potentiallyBufferedInputBytes = receiveBufferSize;
            }

        }

    }

    // TODO: use heuristics based on popular defaults for TCP window sizes
    private void sleepIfRequiredForWrite(int bytesUp) throws ConnectException {

        lastWriteThreadId = Thread.currentThread().getId();

        if (lastReadThreadId == lastWriteThreadId) {
            potentiallyBufferedInputBytes = 0;
        }

        if (0 == sendBufferSize) {
            checkConnectionAllowed(1);
        } else {

            int potentiallyBufferedOutputBytes = this.potentiallyBufferedOutputBytes -= bytesUp;

            if (potentiallyBufferedOutputBytes < 0) {
                int estimatedNumberOfTcpPackets = 1 + (-1 * potentiallyBufferedOutputBytes) / sendBufferSize;
                checkConnectionAllowed(estimatedNumberOfTcpPackets);
                this.potentiallyBufferedOutputBytes = sendBufferSize;
            }

        }

    }

    // TODO: use heuristics based on popular defaults for TCP window sizes
    @IgnoreJRERequirement
    private void estimateReceiveBuffer() {
        if (-1 == receiveBufferSize) {
            if (null == defaultReceiveBufferSize) {
                synchronized (SniffySocketChannel.class) {
                    if (null == defaultReceiveBufferSize) {
                        try {
                            defaultReceiveBufferSize = (Integer) delegate.getOption(StandardSocketOptions.SO_RCVBUF);
                        } catch (SocketException e) {
                            defaultReceiveBufferSize = 0;
                        } catch (IOException e) {
                            defaultReceiveBufferSize = 0;
                        }
                    }
                }
            }
            receiveBufferSize = defaultReceiveBufferSize;
        }
    }

    // TODO: use heuristics based on popular defaults for TCP window sizes
    @IgnoreJRERequirement
    private void estimateSendBuffer() {
        if (-1 == sendBufferSize) {
            if (null == defaultSendBufferSize) {
                synchronized (SniffySocketChannel.class) {
                    if (null == defaultSendBufferSize) {
                        try {
                            defaultSendBufferSize = (Integer) delegate.getOption(StandardSocketOptions.SO_SNDBUF);
                        } catch (SocketException e) {
                            defaultSendBufferSize = 0;
                        } catch (IOException e) {
                            defaultSendBufferSize = 0;
                        }
                    }
                }
            }
            sendBufferSize = defaultSendBufferSize;
        }
    }

    protected void logSocket(long millis) {
        logSocket(millis, 0, 0);
    }

    protected void logSocket(long millis, int bytesDown, int bytesUp) {
        Sniffy.SniffyMode sniffyMode = Sniffy.getSniffyMode();
        if (sniffyMode.isEnabled() && null != getInetSocketAddress() && (millis > 0 || bytesDown > 0 || bytesUp > 0)) {
            Sniffy.logSocket(id, getInetSocketAddress(), millis, bytesDown, bytesUp, sniffyMode.isCaptureStackTraces());
        }
    }

    protected void checkConnectionAllowed() throws ConnectException {
        checkConnectionAllowed(0);
    }

    protected void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException {
        checkConnectionAllowed(getInetSocketAddress(), numberOfSleepCycles);
    }

    protected void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException {
        checkConnectionAllowed(inetSocketAddress, 1);
    }

    protected void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException {
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
    @IgnoreJRERequirement
    public SocketChannel bind(SocketAddress local) throws IOException {
        try {
            copyToDelegate();
            delegate.bind(local);
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        try {
            copyToDelegate();
            delegate.setOption(name, value);
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public SocketChannel shutdownInput() throws IOException {
        try {
            copyToDelegate();
            delegate.shutdownInput();
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public SocketChannel shutdownOutput() throws IOException {
        try {
            copyToDelegate();
            delegate.shutdownOutput();
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public Socket socket() {
        try {
            copyToDelegate();
            return delegate.socket(); // TODO: should we wrap it with SniffySocket ??
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean isConnected() {
        try {
            copyToDelegate();
            return delegate.isConnected();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean isConnectionPending() {
        try {
            copyToDelegate();
            return delegate.isConnectionPending();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed((InetSocketAddress) remote, 1);
            return delegate.connect(remote);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    public boolean finishConnect() throws IOException {
        try {
            copyToDelegate();
            return delegate.finishConnect();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getRemoteAddress() throws IOException {
        try {
            copyToDelegate();
            return delegate.getRemoteAddress();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        estimateReceiveBuffer();
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        try {
            return bytesDown = delegate.read(dst);
        } finally {
            sleepIfRequired(bytesDown);
            logSocket(System.currentTimeMillis() - start, bytesDown, 0);
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return delegate.read(dsts, offset, length); // TODO: handle properly
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        estimateSendBuffer();
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int length = 0;
        try {
            length = delegate.write(src);
            return length;
        } finally {
            sleepIfRequiredForWrite(length);
            logSocket(System.currentTimeMillis() - start, 0, length);
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return delegate.write(srcs, offset, length); // TODO: handle properly
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getLocalAddress() throws IOException {
        copyToDelegate();
        try {
            return delegate.getLocalAddress();
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public void implCloseSelectableChannel() {
        copyToDelegate();
        try {
            invokeMethod(AbstractSelectableChannel.class, delegate, "implCloseSelectableChannel", Void.class);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public void implConfigureBlocking(boolean block) {
        copyToDelegate();
        try {
            invokeMethod(AbstractSelectableChannel.class, delegate, "implConfigureBlocking", Boolean.TYPE, block, Void.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public <T> T getOption(SocketOption<T> name) throws IOException {
        copyToDelegate();
        try {
            return delegate.getOption(name);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public Set<SocketOption<?>> supportedOptions() {
        copyToDelegate();
        try {
            return delegate.supportedOptions();
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Modern SelChImpl

    @Override
    public FileDescriptor getFD() {
        copyToDelegate();
        try {
            return selChImplDelegate.getFD();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public int getFDVal() {
        copyToDelegate();
        try {
            return selChImplDelegate.getFDVal();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        copyToDelegate();
        try {
            return selChImplDelegate.translateAndUpdateReadyOps(ops, ski);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        copyToDelegate();
        try {
            return selChImplDelegate.translateAndSetReadyOps(ops, ski);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public void kill() throws IOException {
        copyToDelegate();
        try {
            selChImplDelegate.kill();
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method is absent in newer JDKs so we cannot use @Override annotation
    // @Override
    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        try {
            copyToDelegate();
            invokeMethod(SelChImpl.class, selChImplDelegate, "translateAndSetInterestOps", Integer.TYPE, ops, SelectionKeyImpl.class, sk, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public int translateInterestOps(int ops) {
        try {
            copyToDelegate();
            return invokeMethod(SelChImpl.class, selChImplDelegate, "translateInterestOps", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public void park(int event, long nanos) throws IOException {
        try {
            copyToDelegate();
            invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Long.TYPE, nanos, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public void park(int event) throws IOException {
        try {
            copyToDelegate();
            invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        } finally {
            copyFromDelegate();
        }
    }

    private static ReflectionFieldCopier[] getReflectionFieldCopiers() {
        if (null == reflectionFieldCopiers) {
            synchronized (SniffySocketChannel.class) {
                if (null == reflectionFieldCopiers) {
                    List<ReflectionFieldCopier> reflectionFieldCopiersList = new ArrayList<ReflectionFieldCopier>(8); // 4 available on modern Java
                    if (keysCopier.isAvailable()) reflectionFieldCopiersList.add(keysCopier);
                    if (keyCountCopier.isAvailable()) reflectionFieldCopiersList.add(keyCountCopier);
                    if (keyLockCopier.isAvailable()) reflectionFieldCopiersList.add(keyLockCopier);
                    if (regLockCopier.isAvailable()) reflectionFieldCopiersList.add(regLockCopier);
                    if (nonBlockingCopier.isAvailable()) reflectionFieldCopiersList.add(nonBlockingCopier);
                    if (blockingCopier.isAvailable()) reflectionFieldCopiersList.add(blockingCopier);
                    if (closeLockCopier.isAvailable()) reflectionFieldCopiersList.add(closeLockCopier);
                    if (closedCopier.isAvailable()) reflectionFieldCopiersList.add(closedCopier);
                    reflectionFieldCopiers = reflectionFieldCopiersList.toArray(new ReflectionFieldCopier[0]);
                }
            }
        }
        return reflectionFieldCopiers;
    }

    private void copyToDelegate() {
        for (ReflectionFieldCopier reflectionFieldCopier : getReflectionFieldCopiers()) {
            reflectionFieldCopier.copy(this, delegate);
        }
    }

    private void copyFromDelegate() {
        for (ReflectionFieldCopier reflectionFieldCopier : getReflectionFieldCopiers()) {
            reflectionFieldCopier.copy(delegate, this);
        }
    }

}
