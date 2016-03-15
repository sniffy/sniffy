package io.sniffy.socket;

import java.io.IOException;
import java.io.InputStream;

public class SnifferInputStream extends InputStream {

    private final SnifferSocketImpl snifferSocket;
    private final InputStream delegate;

    public SnifferInputStream(SnifferSocketImpl snifferSocket, InputStream delegate) {
        this.snifferSocket = snifferSocket;
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        long start = System.currentTimeMillis();
        try {
            return delegate.read();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start, 1, 0);
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        try {
            return bytesDown = delegate.read(b);
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start, bytesDown, 0);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        long start = System.currentTimeMillis();
        int bytesDown = 0;
        try {
            return bytesDown = delegate.read(b, off, len);
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start, bytesDown, 0);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        long start = System.currentTimeMillis();
        try {
            return delegate.skip(n);
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public int available() throws IOException {
        long start = System.currentTimeMillis();
        try {
            return delegate.available();
        } finally {
            snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void close() throws IOException {
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
