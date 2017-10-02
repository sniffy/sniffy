package io.sniffy.socket;

import io.sniffy.registry.ConnectionsRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketOptions;

/**
 * @since 3.1
 */
class SnifferOutputStream extends OutputStream {

    private final SnifferSocketImpl snifferSocket;
    private final OutputStream delegate;

    SnifferOutputStream(SnifferSocketImpl snifferSocket, OutputStream delegate) {
        this.snifferSocket = snifferSocket;
        this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
        } finally {
            sleepIfRequired(1);
            snifferSocket.logSocket(System.currentTimeMillis() - start, 0, 1);
        }
    }

    /**
     * Adds a delay as defined for current {@link SnifferSocketImpl} in {@link ConnectionsRegistry#discoveredDataSources}
     *
     * Delay is added for each <b>N</b> bytes sent where <b>N</b> is the value of {@link SocketOptions#SO_SNDBUF}
     *
     * If application writes <b>M</b> bytes where (k-1) * N &lt; M  &lt; k * N exactly <b>k</b> delays will be added
     *
     * A call to {@link SnifferInputStream} obtained from the same {@link SnifferSocketImpl} and made from the same thread
     * will reset the number of buffered (i.e. which can be written without delay) bytes to 0 effectively adding a guaranteed
     * delay to any subsequent {@link SnifferOutputStream#write(int)} request
     *
     * @param bytesUp number of bytes sent to socket
     * @throws ConnectException on underlying socket exception
     */
    private void sleepIfRequired(int bytesUp) throws ConnectException {

        snifferSocket.lastWriteThreadId = Thread.currentThread().getId();

        if (snifferSocket.lastReadThreadId == snifferSocket.lastWriteThreadId) {
            snifferSocket.potentiallyBufferedInputBytes = 0;
        }

        snifferSocket.potentiallyBufferedOutputBytes = 0;

        if (0 == snifferSocket.sendBufferSize) {
            snifferSocket.checkConnectionAllowed(1);
        } else {

            snifferSocket.potentiallyBufferedOutputBytes -= bytesUp;

            if (snifferSocket.potentiallyBufferedOutputBytes < 0) {
                int estimatedNumberOfTcpPackets = 1 + (-1 * snifferSocket.potentiallyBufferedOutputBytes) / snifferSocket.sendBufferSize;
                snifferSocket.checkConnectionAllowed(estimatedNumberOfTcpPackets);
                snifferSocket.potentiallyBufferedOutputBytes = snifferSocket.sendBufferSize;
            }

        }

    }

    @Override
    public void write(byte[] b) throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
        } finally {
            sleepIfRequired(b.length);
            snifferSocket.logSocket(System.currentTimeMillis() - start, 0, b.length);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.write(b, off, len);
        } finally {
            sleepIfRequired(len);
            snifferSocket.logSocket(System.currentTimeMillis() - start, 0, len);
        }
    }

    @Override
    public void flush() throws IOException {
        snifferSocket.checkConnectionAllowed(1);
        long start = System.currentTimeMillis();
        try {
            delegate.flush();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void close() throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.close();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

}
