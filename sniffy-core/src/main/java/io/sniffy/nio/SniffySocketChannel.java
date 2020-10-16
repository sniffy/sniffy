package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.SniffySocket;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionFieldCopier;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.SocketChannelDelegate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

// TODO: SelChImpl is available in java 1.7+ only - make sure it is safe
public class SniffySocketChannel extends SocketChannelDelegate implements SniffySocket {

    private final static ReflectionFieldCopier providerCopier =
            new ReflectionFieldCopier(AbstractSelectableChannel.class, "provider");
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

    private final static ReflectionFieldCopier closeLockCopier =
            new ReflectionFieldCopier(AbstractInterruptibleChannel.class, "closeLock");
    private final static ReflectionFieldCopier closedCopier =
            new ReflectionFieldCopier(AbstractInterruptibleChannel.class, "closed");

    private static volatile ReflectionFieldCopier[] reflectionFieldCopiers;

    private final SocketChannel delegate;

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
        super(provider, delegate);
        this.delegate = delegate;
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


    /**
     * Adds a delay as defined for current {@link SnifferSocketImpl} in {@link ConnectionsRegistry#discoveredDataSources}
     * <p>
     * Delay is added for each <b>N</b> bytes received where <b>N</b> is the value of {@link SocketOptions#SO_RCVBUF}
     * <p>
     * If application reads <b>M</b> bytes where (k-1) * N &lt; M  &lt; k * N exactly <b>k</b> delays will be added
     * <p>
     * A call to {@link SnifferOutputStream} obtained from the same {@link SnifferSocketImpl} and made from the same thread
     * will reset the number of buffered (i.e. which can be read without delay) bytes to 0 effectively adding a guaranteed
     * delay to any subsequent {@link SnifferInputStream#read()} request
     * <p>
     * TODO: consider if {@link java.net.SocketInputStream#available()} method can be of any use here
     *
     * @param bytesDown number of bytes received from socket
     * @throws ConnectException on underlying socket exception
     */
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

    /**
     * Adds a delay as defined for current {@link SnifferSocketImpl} in {@link ConnectionsRegistry#discoveredDataSources}
     * <p>
     * Delay is added for each <b>N</b> bytes sent where <b>N</b> is the value of {@link SocketOptions#SO_SNDBUF}
     * <p>
     * If application writes <b>M</b> bytes where (k-1) * N &lt; M  &lt; k * N exactly <b>k</b> delays will be added
     * <p>
     * A call to {@link SnifferInputStream} obtained from the same {@link SnifferSocketImpl} and made from the same thread
     * will reset the number of buffered (i.e. which can be written without delay) bytes to 0 effectively adding a guaranteed
     * delay to any subsequent {@link SnifferOutputStream#write(int)} request
     *
     * @param bytesUp number of bytes sent to socket
     * @throws ConnectException on underlying socket exception
     */
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
            method(Class.forName("java.nio.channels.NetworkChannel"), "bind", SocketAddress.class).invoke(delegate, local);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.processException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.processException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.processException(e);
        } catch (ClassNotFoundException e) {
            throw ExceptionUtil.processException(e);
        }
        return this;
    }

    @Override
    @IgnoreJRERequirement
    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        try {
            method(Class.forName("java.nio.channels.NetworkChannel"), "setOption", SocketOption.class, Object.class).invoke(delegate, name, value);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.processException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.processException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.processException(e);
        } catch (ClassNotFoundException e) {
            throw ExceptionUtil.processException(e);
        }
        return this;
    }

    @Override
    @IgnoreJRERequirement
    public SocketChannel shutdownInput() throws IOException {
        delegate.shutdownInput();
        return this;
    }

    @Override
    @IgnoreJRERequirement
    public SocketChannel shutdownOutput() throws IOException {
        delegate.shutdownOutput();
        return this;
    }

    @Override
    public Socket socket() {
        return delegate.socket();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean isConnectionPending() {
        return delegate.isConnectionPending();
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
        return delegate.finishConnect();
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getRemoteAddress() throws IOException {
        return delegate.getRemoteAddress();
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
        return delegate.read(dsts, offset, length);
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
        return delegate.write(srcs, offset, length);
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getLocalAddress() throws IOException {
        try {
            return (SocketAddress) method(Class.forName("java.nio.channels.NetworkChannel"), "getLocalAddress").invoke(delegate);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.processException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.processException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.processException(e);
        } catch (ClassNotFoundException e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    public void implCloseSelectableChannel() {
        copyToDelegate();
        try {
            method(AbstractSelectableChannel.class, "implCloseSelectableChannel").invoke(delegate);
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
            method(AbstractSelectableChannel.class, "implConfigureBlocking", boolean.class).invoke(delegate, block);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // TODO: it wouldn't work before Java 1.7 - shall we drop NIO support in Java 1.6 at all?
    @Override
    @IgnoreJRERequirement
    public <T> T getOption(SocketOption<T> name) throws IOException {
        try {
            return (T) method(Class.forName("java.nio.channels.NetworkChannel"), "getOption", SocketOption.class).invoke(delegate, name);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.processException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.processException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.processException(e);
        } catch (ClassNotFoundException e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    @IgnoreJRERequirement
    public Set<SocketOption<?>> supportedOptions() {
        try {
            return (Set<SocketOption<?>>) method(Class.forName("java.nio.channels.NetworkChannel"), "supportedOptions").invoke(delegate);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.processException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.processException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.processException(e);
        } catch (ClassNotFoundException e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Modern SelChImpl

/*
    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public int translateInterestOps(int ops) {
        try {
            return (Integer) method(SelChImpl.class, "translateInterestOps", Integer.TYPE).invoke(delegate, ops);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.processException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.processException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public void park(int event, long nanos) throws IOException {
        try {
            method(SelChImpl.class, "park", Integer.TYPE, Long.TYPE).invoke(selChImplDelegate, event, nanos);
        } catch (IllegalAccessException e) {
            ExceptionUtil.throwException(e);
        } catch (InvocationTargetException e) {
            ExceptionUtil.throwException(e);
        } catch (NoSuchMethodException e) {
            ExceptionUtil.throwException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public void park(int event) throws IOException {
        try {
            method(SelChImpl.class, "park", Integer.TYPE).invoke(selChImplDelegate, event);
        } catch (IllegalAccessException e) {
            ExceptionUtil.throwException(e);
        } catch (InvocationTargetException e) {
            ExceptionUtil.throwException(e);
        } catch (NoSuchMethodException e) {
            ExceptionUtil.throwException(e);
        }
    }
*/

    private static ReflectionFieldCopier[] getReflectionFieldCopiers() {
        if (null == reflectionFieldCopiers) {
            synchronized (SniffySocketChannel.class) {
                if (null == reflectionFieldCopiers) {
                    List<ReflectionFieldCopier> reflectionFieldCopiersList = new ArrayList<ReflectionFieldCopier>(8); // 4 available on modern Java
                    if (providerCopier.isAvailable()) reflectionFieldCopiersList.add(providerCopier);
                    if (keysCopier.isAvailable()) reflectionFieldCopiersList.add(keysCopier);
                    if (keyCountCopier.isAvailable()) reflectionFieldCopiersList.add(keyCountCopier);
                    if (keyLockCopier.isAvailable()) reflectionFieldCopiersList.add(keyLockCopier);
                    if (regLockCopier.isAvailable()) reflectionFieldCopiersList.add(regLockCopier);
                    if (nonBlockingCopier.isAvailable()) reflectionFieldCopiersList.add(nonBlockingCopier);
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

    private static Method method(Class<?> clazz, String methodName, Class<?>... argumentTypes) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, argumentTypes);
        method.setAccessible(true);
        return method;
    }

}
