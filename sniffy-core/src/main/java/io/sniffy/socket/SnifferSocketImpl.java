package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionFieldCopier;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 3.1
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
class SnifferSocketImpl extends SocketImpl {

    private final static ReflectionFieldCopier socketCopier =
            new ReflectionFieldCopier(SocketImpl.class, "socket");
    private final static ReflectionFieldCopier serverSocketCopier =
            new ReflectionFieldCopier(SocketImpl.class, "serverSocket");
    private final static ReflectionFieldCopier fdCopier =
            new ReflectionFieldCopier(SocketImpl.class, "fd");
    private final static ReflectionFieldCopier addressCopier =
            new ReflectionFieldCopier(SocketImpl.class, "address");
    private final static ReflectionFieldCopier portCopier =
            new ReflectionFieldCopier(SocketImpl.class, "port");
    private final static ReflectionFieldCopier localportCopier =
            new ReflectionFieldCopier(SocketImpl.class, "localport");

    private static volatile ReflectionFieldCopier[] reflectionFieldCopiers;

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

    protected SnifferSocketImpl(SocketImpl delegate) {
        this.delegate = delegate;
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

    protected static void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException {
        if (null != inetSocketAddress) {
            int status = ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(inetSocketAddress);
            if (status < 0) {
                if (numberOfSleepCycles > 0 && -1 != status) try {
                    sleepImpl(-1 * status * numberOfSleepCycles);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                throw new ConnectException(String.format("Connection to %s refused by Sniffy", inetSocketAddress));
            } else if (numberOfSleepCycles > 0 && status > 0) {
                try {
                    sleepImpl(status * numberOfSleepCycles);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void sleepImpl(int millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private static ReflectionFieldCopier[] getReflectionFieldCopiers() {
        if (null == reflectionFieldCopiers) {
            synchronized (SnifferSocketImpl.class) {
                if (null == reflectionFieldCopiers) {
                    List<ReflectionFieldCopier> reflectionFieldCopiersList = new ArrayList<ReflectionFieldCopier>(6);
                    if (socketCopier.isAvailable()) reflectionFieldCopiersList.add(socketCopier);
                    if (serverSocketCopier.isAvailable()) reflectionFieldCopiersList.add(serverSocketCopier);
                    if (fdCopier.isAvailable()) reflectionFieldCopiersList.add(fdCopier);
                    if (addressCopier.isAvailable()) reflectionFieldCopiersList.add(addressCopier);
                    if (portCopier.isAvailable()) reflectionFieldCopiersList.add(portCopier);
                    if (localportCopier.isAvailable()) reflectionFieldCopiersList.add(localportCopier);
                    reflectionFieldCopiers = reflectionFieldCopiersList.toArray(new ReflectionFieldCopier[6]);
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

    private static Method method(String methodName, Class<?>... argumentTypes) throws NoSuchMethodException {
        Method method = SocketImpl.class.getDeclaredMethod(methodName, argumentTypes);
        method.setAccessible(true);
        return method;
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        copyToDelegate();
        long start = System.currentTimeMillis();
        try {
            checkConnectionAllowed(1);
            method("sendUrgentData", int.class).invoke(delegate, data);
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
            method("shutdownInput").invoke(delegate);
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
            method("shutdownOutput").invoke(delegate);
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
            return (FileDescriptor) method("getFileDescriptor").invoke(delegate);
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
            return (InetAddress) method("getInetAddress").invoke(delegate);
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
            return (Integer) method("getPort").invoke(delegate);
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
            return (Boolean) method("supportsUrgentData").invoke(delegate);
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
            return (Integer) method("getLocalPort").invoke(delegate);
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
            method("setPerformancePreferences", int.class, int.class, int.class).invoke(delegate, connectionTime, latency, bandwidth);
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
            method("create", boolean.class).invoke(delegate, stream);
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
            method("connect", String.class, int.class).invoke(delegate, host, port);
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
            method("connect", InetAddress.class, int.class).invoke(delegate, address, port);
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
            method("connect", SocketAddress.class, int.class).invoke(delegate, address, timeout);
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
            method("bind", InetAddress.class, int.class).invoke(delegate, host, port);
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
            method("listen", int.class).invoke(delegate, backlog);
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
            method("accept", SocketImpl.class).invoke(delegate, s);
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
            return new SnifferInputStream(this, (InputStream) method("getInputStream").invoke(delegate));
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
            return new SnifferOutputStream(this, (OutputStream) method("getOutputStream").invoke(delegate));
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
            return (Integer) method("available").invoke(delegate);
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
            method("close").invoke(delegate);
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
}
