package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.SpyConfiguration;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.socket.SniffySocket;
import io.sniffy.util.ExceptionUtil;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

import static io.sniffy.Sniffy.CONNECTION_ID_SEQUENCE;

/**
 * @since 3.1.7
 */
public class SniffySocketChannel extends SniffySocketChannelAdapter implements SniffyNetworkConnection {

    private final int connectionId = CONNECTION_ID_SEQUENCE.getAndIncrement();

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

    protected SniffySocketChannel(SelectorProvider provider, SocketChannel delegate) {
        super(provider, delegate);
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
                            defaultReceiveBufferSize = super.getOption(StandardSocketOptions.SO_RCVBUF);
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
                            defaultSendBufferSize = super.getOption(StandardSocketOptions.SO_SNDBUF);
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

    @Deprecated
    public void logSocket(long millis) {
        logSocket(millis, 0, 0);
    }

    @Deprecated
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
            Sniffy.logTraffic(
                    connectionId, getInetSocketAddress(),
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

    private static void sleepImpl(int millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        estimateReceiveBuffer();
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        int position = dst.position();
        try {
            return bytesDown = super.read(dst);
        } finally {
            if (bytesDown >= 0) { // TODO: implement same check in other places
                sleepIfRequired(bytesDown);
                logSocket(System.currentTimeMillis() - start, bytesDown, 0);
                SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
                if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
                    dst.position(position);
                    byte[] buff = new byte[bytesDown];
                    dst.get(buff, 0, bytesDown);

                    logTraffic(false, Protocol.TCP, buff, 0, buff.length);
                }
            }
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        estimateReceiveBuffer();
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        long bytesDown = 0;

        int[] positions = new int[length];
        int[] remainings = new int[length];

        for (int i = 0; i < length; i++) {
            positions[i] = dsts[offset + i].position();
            remainings[i] = dsts[offset + i].remaining();
        }

        try {
            bytesDown = super.read(dsts, offset, length);
            return bytesDown;
        } finally {
            while (bytesDown > Integer.MAX_VALUE) {
                sleepIfRequiredForWrite(Integer.MAX_VALUE);
                logSocket(System.currentTimeMillis() - start, Integer.MAX_VALUE, 0);
                bytesDown -= Integer.MAX_VALUE;
            }
            logSocket(System.currentTimeMillis() - start, (int) bytesDown, 0);

            SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
            if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
                for (int i = 0; i < length; i++) {
                    dsts[offset + i].position(positions[i]);
                    byte[] buff = new byte[remainings[i]];
                    dsts[offset + i].get(buff, 0, remainings[i]);
                    logTraffic(false, Protocol.TCP, buff, 0, buff.length);
                }

            }
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        estimateSendBuffer();
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int length = 0;

        int position = src.position();

        try {
            length = super.write(src);
            return length;
        } finally {
            sleepIfRequiredForWrite(length);
            logSocket(System.currentTimeMillis() - start, 0, length);
            SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
            if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
                src.position(position);
                byte[] buff = new byte[length];
                src.get(buff, 0, length);
                logTraffic(true, Protocol.TCP, buff, 0, buff.length);
            }
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        estimateSendBuffer();
        checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        long bytesUp = 0;

        int[] positions = new int[length];
        int[] remainings = new int[length];

        for (int i = 0; i < length; i++) {
            positions[i] = srcs[offset + i].position();
            remainings[i] = srcs[offset + i].remaining();
        }

        try {
            bytesUp = super.write(srcs, offset, length);
            return bytesUp;
        } finally {
            while (bytesUp > Integer.MAX_VALUE) {
                sleepIfRequiredForWrite(Integer.MAX_VALUE);
                logSocket(System.currentTimeMillis() - start, 0, Integer.MAX_VALUE);
                bytesUp -= Integer.MAX_VALUE;
            }
            sleepIfRequiredForWrite((int) bytesUp);
            logSocket(System.currentTimeMillis() - start, 0, (int) bytesUp);
            SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
            if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
                for (int i = 0; i < length; i++) {
                    srcs[offset + i].position(positions[i]);
                    byte[] buff = new byte[remainings[i]];
                    srcs[offset + i].get(buff, 0, remainings[i]);
                    logTraffic(true, Protocol.TCP, buff, 0, buff.length);
                }

            }
        }
    }

    @Override
    public Socket socket() {
        try {
            return new SniffySocket(super.socket(), this, connectionId, getInetSocketAddress());
        } catch (SocketException e) {
            e.printStackTrace();
            return super.socket();
        }
    }

    //

    @Override
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    @Override
    public int getSendBufferSize() {
        return sendBufferSize;
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

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
