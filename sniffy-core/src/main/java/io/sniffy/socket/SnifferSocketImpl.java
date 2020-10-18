package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionCopier;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * @since 3.1
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
class SnifferSocketImpl extends SocketImpl implements SniffySocket {

    private static final ReflectionCopier<SocketImpl> socketChannelFieldsCopier = new ReflectionCopier<SocketImpl>(SocketImpl.class);

    private final SocketImpl delegate;

    private InetSocketAddress address;

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

    private volatile Integer connectionStatus;

    protected SnifferSocketImpl(SocketImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setConnectionStatus(Integer connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    private void estimateReceiveBuffer() {
        if (-1 == receiveBufferSize) {
            if (null == defaultReceiveBufferSize) {
                synchronized (SnifferSocketImpl.class) {
                    if (null == defaultReceiveBufferSize) {
                        try {
                            Object o = delegate.getOption(SocketOptions.SO_RCVBUF);
                            if (o instanceof Integer) {
                                defaultReceiveBufferSize = (Integer) o;
                            } else {
                                defaultReceiveBufferSize = 0;
                            }
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
                            Object o = delegate.getOption(SocketOptions.SO_SNDBUF);
                            if (o instanceof Integer) {
                                defaultSendBufferSize = (Integer) o;
                            } else {
                                defaultSendBufferSize = 0;
                            }
                        } catch (SocketException e) {
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
        if (sniffyMode.isEnabled() && null != address && (millis > 0 || bytesDown > 0 || bytesUp > 0)) {
            Sniffy.logSocket(id, address, millis, bytesDown, bytesUp, sniffyMode.isCaptureStackTraces());
        }
    }

    protected void checkConnectionAllowed() throws ConnectException {
        checkConnectionAllowed(0);
    }

    protected void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException {
        checkConnectionAllowed(address, numberOfSleepCycles);
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

    private void copyToDelegate() {
        socketChannelFieldsCopier.copy(this, delegate);
    }

    private void copyFromDelegate() {
        socketChannelFieldsCopier.copy(delegate, this);
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed(1);
            invokeMethod(SocketImpl.class, delegate, "sendUrgentData", int.class, data, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void shutdownInput() throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            invokeMethod(SocketImpl.class, delegate, "shutdownInput", Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void shutdownOutput() throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            invokeMethod(SocketImpl.class, delegate, "shutdownOutput", Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected FileDescriptor getFileDescriptor() {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getFileDescriptor", FileDescriptor.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected InetAddress getInetAddress() {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getInetAddress", InetAddress.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected int getPort() {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getPort", Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected boolean supportsUrgentData() {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return invokeMethod(SocketImpl.class, delegate, "supportsUrgentData", Boolean.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected int getLocalPort() {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getLocalPort", Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    public String toString() {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return delegate.toString();
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    // TODO: wrap equals and hashCode methods

    @Override
    protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            invokeMethod(SocketImpl.class, delegate, "setPerformancePreferences", Integer.TYPE, connectionTime, Integer.TYPE, latency, Integer.TYPE, bandwidth, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void create(boolean stream) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            invokeMethod(SocketImpl.class, delegate, "create", Boolean.TYPE, stream, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed(this.address = new InetSocketAddress(host, port));
            invokeMethod(SocketImpl.class, delegate, "connect", String.class, host, Integer.TYPE, port, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed(this.address = new InetSocketAddress(address, port));
            invokeMethod(SocketImpl.class, delegate, "connect", InetAddress.class, address, Integer.TYPE, port, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            if (address instanceof InetSocketAddress) {
                checkConnectionAllowed(this.address = (InetSocketAddress) address);
            }
            invokeMethod(SocketImpl.class, delegate, "connect", SocketAddress.class, address, Integer.TYPE, port, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            invokeMethod(SocketImpl.class, delegate, "bind", InetAddress.class, host, Integer.TYPE, port, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void listen(int backlog) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            invokeMethod(SocketImpl.class, delegate, "listen", Integer.TYPE, backlog, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            invokeMethod(SocketImpl.class, delegate, "accept", SocketImpl.class, s, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        estimateReceiveBuffer();
        checkConnectionAllowed();
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return new SnifferInputStream(this, invokeMethod(SocketImpl.class, delegate, "getInputStream", InputStream.class));
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        estimateSendBuffer();
        checkConnectionAllowed();
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return new SnifferOutputStream(this, invokeMethod(SocketImpl.class, delegate, "getOutputStream", OutputStream.class));
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected int available() throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return invokeMethod(SocketImpl.class, delegate, "available", Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    protected void close() throws IOException {
        copyToDelegate();
        checkConnectionAllowed(1);
        long start = System.currentTimeMillis();
        try {
            invokeMethod(SocketImpl.class, delegate, "close", Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    // interface

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            delegate.setOption(optID, value);

            if (SocketOptions.SO_RCVBUF == optID) {
                try {
                    receiveBufferSize = ((Number) value).intValue();
                } catch (Exception e) {
                    // TODO: log me maybe
                }
            } else if (SocketOptions.SO_SNDBUF == optID) {
                try {
                    sendBufferSize = ((Number) value).intValue();
                } catch (Exception e) {
                    // TODO: log me maybe
                }
            }

        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            return delegate.getOption(optID);
        } finally {
            logSocket(System.currentTimeMillis() - start);
            copyFromDelegate();
        }
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        return this.address;
    }

}
