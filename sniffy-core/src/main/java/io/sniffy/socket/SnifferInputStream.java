package io.sniffy.socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;

/**
 * @since 3.1
 */
class SnifferInputStream extends InputStream {

    private final SnifferSocketImpl snifferSocket;
    private final InputStream delegate;

    private int potentiallyBufferedBytes = 0; // TODO: move to SnifferSocketImpl

    SnifferInputStream(SnifferSocketImpl snifferSocket, InputStream delegate) {
        this.snifferSocket = snifferSocket;
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        try {
            int read = delegate.read();
            if (read != -1) bytesDown = 1;
            return read;
        } finally {
            sleepIfRequired(bytesDown);
            snifferSocket.logSocket(System.currentTimeMillis() - start, bytesDown, 0);
        }
    }

    private void sleepIfRequired(int bytesDown) throws ConnectException {

        if (0 == snifferSocket.receiveBufferSize) {
            snifferSocket.checkConnectionAllowed(1);
        } else {

            potentiallyBufferedBytes -= bytesDown;

            if (potentiallyBufferedBytes < 0) {
                int estimatedNumberOfTcpPackets = 1 + (-1 * potentiallyBufferedBytes) / snifferSocket.receiveBufferSize;
                snifferSocket.checkConnectionAllowed(estimatedNumberOfTcpPackets);
                potentiallyBufferedBytes = snifferSocket.receiveBufferSize;
            }

        }

    }

    @Override
    public int read(byte[] b) throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        try {
            return bytesDown = delegate.read(b);
        } finally {
            sleepIfRequired(bytesDown);
            snifferSocket.logSocket(System.currentTimeMillis() - start, bytesDown, 0);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        try {
            return bytesDown = delegate.read(b, off, len);
        } finally {
            sleepIfRequired(bytesDown);
            snifferSocket.logSocket(System.currentTimeMillis() - start, bytesDown, 0);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            return delegate.skip(n);
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public int available() throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            return delegate.available();
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

    @Override
    public void mark(int readlimit) {
        long start = System.currentTimeMillis();
        try {
            delegate.mark(readlimit);
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void reset() throws IOException {
        snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.reset();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public boolean markSupported() {
        long start = System.currentTimeMillis();
        try {
            return delegate.markSupported();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

}
