package io.sniffy.socket;

import io.sniffy.registry.ConnectionsRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;

/**
 * @since 3.1
 */
public class SnifferOutputStream extends OutputStream {

    // TODO: refactor
    private final SniffyNetworkConnection snifferSocket;
    private final TrafficCapturingNetworkConnection trafficCapturingNetworkConnection;
    private final OutputStream delegate;

    public SnifferOutputStream(TrafficCapturingNetworkConnection trafficCapturingNetworkConnection, OutputStream delegate) {
        this.trafficCapturingNetworkConnection = trafficCapturingNetworkConnection;
        if (trafficCapturingNetworkConnection instanceof SniffyNetworkConnection) {
            this.snifferSocket = (SniffyNetworkConnection) trafficCapturingNetworkConnection;
        } else {
            this.snifferSocket = null;
        }
        this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
            trafficCapturingNetworkConnection.logTraffic(
                    true, Protocol.TCP,
                    new byte[]{(byte) b},
                    0, 1
            );
        } finally {
            sleepIfRequired(1);
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start, 0, 1);
        }
    }

    /**
     * Adds a delay as defined for current {@link SnifferSocketImpl} in {@link ConnectionsRegistry#discoveredDataSources}
     * <p>
     * Delay is added for each <b>N</b> bytes sent where <b>N</b> is the value of {@link SniffyNetworkConnection#DEFAULT_TCP_WINDOW_SIZE}
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
    @SuppressWarnings("JavadocReference")
    private void sleepIfRequired(int bytesUp) throws ConnectException {

        if (null == snifferSocket) return;

        snifferSocket.setLastWriteThreadId(Thread.currentThread().getId());

        if (snifferSocket.getLastReadThreadId() == snifferSocket.getLastWriteThreadId()) {
            snifferSocket.setPotentiallyBufferedInputBytes(0);
        }

        int potentiallyBufferedOutputBytes = snifferSocket.getPotentiallyBufferedOutputBytes() - bytesUp;
        snifferSocket.setPotentiallyBufferedOutputBytes(potentiallyBufferedOutputBytes);

        if (potentiallyBufferedOutputBytes < 0) {
            int estimatedNumberOfTcpPackets = 1 + (-1 * potentiallyBufferedOutputBytes) / SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
            snifferSocket.checkConnectionAllowed(estimatedNumberOfTcpPackets);
            snifferSocket.setPotentiallyBufferedOutputBytes(SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE);
        }

    }

    @Override
    public void write(byte[] b) throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
            trafficCapturingNetworkConnection.logTraffic(
                    true, Protocol.TCP,
                    b,
                    0, b.length
            );
        } finally {
            sleepIfRequired(b.length);
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start, 0, b.length);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.write(b, off, len);
            trafficCapturingNetworkConnection.logTraffic(
                    true, Protocol.TCP,
                    b,
                    off, len
            );
        } finally {
            sleepIfRequired(len);
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start, 0, len);
        }
    }

    @Override
    public void flush() throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(1);
        long start = System.currentTimeMillis();
        try {
            delegate.flush();
        } finally {
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void close() throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.close();
        } finally {
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

}
