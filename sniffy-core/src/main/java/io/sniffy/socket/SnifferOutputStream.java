package io.sniffy.socket;

import java.io.IOException;
import java.io.OutputStream;

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
        snifferSocket.checkConnectionAllowed();
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start, 0, 1);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        snifferSocket.checkConnectionAllowed();
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start, 0, b.length);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        snifferSocket.checkConnectionAllowed();
        long start = System.currentTimeMillis();
        try {
            delegate.write(b, off, len);
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start, 0, len);
        }
    }

    @Override
    public void flush() throws IOException {
        snifferSocket.checkConnectionAllowed();
        long start = System.currentTimeMillis();
        try {
            delegate.flush();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void close() throws IOException {
        snifferSocket.checkConnectionAllowed();
        long start = System.currentTimeMillis();
        try {
            delegate.close();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

}
