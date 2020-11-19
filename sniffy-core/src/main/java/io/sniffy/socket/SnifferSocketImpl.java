package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.registry.ConnectionsRegistry;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

/**
 * @since 3.1
 */
class SnifferSocketImpl extends SniffySocketImplAdapter implements SniffyNetworkConnection {

    private final Sleep sleep;

    private InetSocketAddress address;

    private final int id = Sniffy.CONNECTION_ID_SEQUENCE.getAndIncrement();

    private volatile Integer connectionStatus;

    // fields related to injecting latency fault

    protected static volatile Integer defaultReceiveBufferSize;
    protected static volatile Integer defaultSendBufferSize;

    private int receiveBufferSize = -1;
    private int sendBufferSize = -1;

    private volatile int potentiallyBufferedInputBytes = 0;
    private volatile int potentiallyBufferedOutputBytes = 0;

    private volatile long lastReadThreadId;
    private volatile long lastWriteThreadId;

    protected SnifferSocketImpl(SocketImpl delegate, Sleep sleep) {
        super(delegate);
        this.sleep = sleep;
    }

    protected SnifferSocketImpl(SocketImpl delegate) {
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

    private void estimateReceiveBuffer() {
        if (-1 == getReceiveBufferSize()) {
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
            setReceiveBufferSize(defaultReceiveBufferSize);
        }
    }

    private void estimateSendBuffer() {
        if (-1 == getSendBufferSize()) {
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
            setSendBufferSize(defaultSendBufferSize);
        }
    }

    public void logSocket(long millis) {
        logSocket(millis, 0, 0);
    }

    public void logSocket(long millis, int bytesDown, int bytesUp) {
        if (null != address && (millis > 0 || bytesDown > 0 || bytesUp > 0)) {
            Sniffy.SniffyMode sniffyMode = Sniffy.getSniffyMode();
            if (sniffyMode.isEnabled()) {
                Sniffy.logSocket(id, address, millis, bytesDown, bytesUp, sniffyMode.isCaptureStackTraces());
            }
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

    // TODO: inverse this check; otherwise it is too slow
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
            logSocket(System.currentTimeMillis() - start);
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
        estimateReceiveBuffer();
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
        estimateSendBuffer();
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

            if (SocketOptions.SO_RCVBUF == optID) {
                try {
                    setReceiveBufferSize(((Number) value).intValue());
                } catch (Exception e) {
                    // TODO: log me maybe
                }
            } else if (SocketOptions.SO_SNDBUF == optID) {
                try {
                    setSendBufferSize(((Number) value).intValue());
                } catch (Exception e) {
                    // TODO: log me maybe
                }
            }

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

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    // New methods in Java 9
    // TODO: use multi-release jars to handle it

    /*
    @Override
    protected <T> void setOption(java.net.SocketOption<T> name, T value) throws IOException {
        long start = System.currentTimeMillis();
        try {
            super.setOption(name, value);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected <T> T getOption(java.net.SocketOption<T> name) throws IOException {
        long start = System.currentTimeMillis();
        try {
            return super.getOption(name);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    protected Set<java.net.SocketOption<?>> supportedOptions() {
        long start = System.currentTimeMillis();
        try {
            return super.supportedOptions();
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }
    */

}
