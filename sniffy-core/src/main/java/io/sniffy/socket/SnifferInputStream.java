package io.sniffy.socket;

import io.sniffy.registry.ConnectionsRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;

/**
 * @since 3.1
 */
public class SnifferInputStream extends InputStream {

    // TODO: refactor
    private final SniffyNetworkConnection snifferSocket;
    private final TrafficCapturingNetworkConnection trafficCapturingNetworkConnection;
    private final InputStream delegate;

    public SnifferInputStream(TrafficCapturingNetworkConnection trafficCapturingNetworkConnection, InputStream delegate) {
        this.trafficCapturingNetworkConnection = trafficCapturingNetworkConnection;
        if (trafficCapturingNetworkConnection instanceof SniffyNetworkConnection) {
            this.snifferSocket = (SniffyNetworkConnection) trafficCapturingNetworkConnection;
        } else {
            this.snifferSocket = null;
        }
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        try {
            int read = delegate.read();
            if (read != -1) {
                bytesDown = 1;
                trafficCapturingNetworkConnection.logTraffic(
                        false, Protocol.TCP,
                        new byte[]{(byte) read},
                        0, 1
                );
            }
            return read;
        } finally {
            sleepIfRequired(bytesDown);
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start, bytesDown, 0);
        }
    }

    /**
     * Adds a delay as defined for current {@link SnifferSocketImpl} in {@link ConnectionsRegistry#discoveredDataSources}
     * <p>
     * Delay is added for each <b>N</b> bytes received where <b>N</b> is the value of {@link SniffyNetworkConnection#DEFAULT_TCP_WINDOW_SIZE}
     * <p>
     * If application reads <b>M</b> bytes where (k-1) * N &lt; M  &lt; k * N exactly <b>k</b> delays will be added
     * <p>
     * A call to {@link SnifferOutputStream} obtained from the same {@link SnifferSocketImpl} and made from the same thread
     * will reset the number of buffered (i.e. which can be read without delay) bytes to 0 effectively adding a guaranteed
     * delay to any subsequent {@link SnifferInputStream#read()} request
     * <p>
     * TODO: consider if {@link java.net.SocketInputStream#available()} } method can be of any use here
     *
     * @param bytesDown number of bytes received from socket
     * @throws ConnectException on underlying socket exception
     */
    @SuppressWarnings("JavadocReference")
    private void sleepIfRequired(int bytesDown) throws ConnectException {

        if (null == snifferSocket) return;

        snifferSocket.setLastReadThreadId(Thread.currentThread().getId());

        if (snifferSocket.getLastReadThreadId() == snifferSocket.getLastWriteThreadId()) {
            snifferSocket.setPotentiallyBufferedOutputBytes(0);
        }

        int potentiallyBufferedInputBytes = snifferSocket.getPotentiallyBufferedInputBytes() - bytesDown;
        snifferSocket.setPotentiallyBufferedInputBytes(potentiallyBufferedInputBytes);

        if (potentiallyBufferedInputBytes < 0) {
            int estimatedNumberOfTcpPackets = 1 + (-1 * potentiallyBufferedInputBytes) / SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
            snifferSocket.checkConnectionAllowed(estimatedNumberOfTcpPackets);
            snifferSocket.setPotentiallyBufferedInputBytes(SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE);
        }

    }

    @Override
    public int read(byte[] b) throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        try {
            bytesDown = delegate.read(b);
            if (-1 != bytesDown) {
                trafficCapturingNetworkConnection.logTraffic(
                        false, Protocol.TCP,
                        b,
                        0, bytesDown
                );
            }
            return bytesDown;
        } finally {
            sleepIfRequired(bytesDown);
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start, bytesDown, 0);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        try {
            bytesDown = delegate.read(b, off, len);
            if (-1 != bytesDown) {
                trafficCapturingNetworkConnection.logTraffic(
                        false, Protocol.TCP,
                        b,
                        off, bytesDown
                );
            }
            return bytesDown;
        } finally {
            sleepIfRequired(bytesDown);
            //snifferSocket.logTraffic(false, Protocol.TCP, b, off, bytesDown); // TODO
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start, bytesDown, 0);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            return super.skip(n);
        } finally {
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public int available() throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            return delegate.available();
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

    @Override
    public void mark(int readlimit) {
        // TODO: support this method in case it is supported in future by SocketInputStream; at least print a warning
        long start = System.currentTimeMillis();
        try {
            delegate.mark(readlimit);
        } finally {
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void reset() throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.reset();
        } finally {
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public boolean markSupported() {
        // TODO: support this method in case it is supported in future by SocketInputStream; at least print a warning
        long start = System.currentTimeMillis();
        try {
            return delegate.markSupported();
        } finally {
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

}
