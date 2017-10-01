package io.sniffy.socket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;

/**
 * @since 3.1
 */
class SnifferOutputStream extends OutputStream {

    private final SnifferSocketImpl snifferSocket;
    private final OutputStream delegate;

    private int potentiallyBufferedBytes = 0;

    SnifferOutputStream(SnifferSocketImpl snifferSocket, OutputStream delegate) {
        this.snifferSocket = snifferSocket;
        this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
        snifferSocket.checkConnectionAllowed(false);
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
        } finally {
            sleepIfRequired(1);
            snifferSocket.logSocket(System.currentTimeMillis() - start, 0, 1);
        }
    }

    private void sleepIfRequired(int bytesUp) throws ConnectException {
        potentiallyBufferedBytes -= bytesUp;

        if (potentiallyBufferedBytes < 0) { // TODO: sleep multiple times if required
            snifferSocket.checkConnectionAllowed(true);
            potentiallyBufferedBytes = snifferSocket.receiveBufferSize;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        snifferSocket.checkConnectionAllowed(false);
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
        snifferSocket.checkConnectionAllowed(false);
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
        snifferSocket.checkConnectionAllowed(true);
        long start = System.currentTimeMillis();
        try {
            delegate.flush();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void close() throws IOException {
        snifferSocket.checkConnectionAllowed(false);
        long start = System.currentTimeMillis();
        try {
            delegate.close();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

}
