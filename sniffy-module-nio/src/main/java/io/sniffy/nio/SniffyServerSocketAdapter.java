package io.sniffy.nio;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ServerSocketChannel;

public class SniffyServerSocketAdapter extends ServerSocket {

    private final ServerSocket delegate;

    public SniffyServerSocketAdapter(ServerSocket delegate) throws IOException {
        super();
        this.delegate = delegate;
    }

    @Override
    public void bind(SocketAddress endpoint) throws IOException {
        delegate.bind(endpoint);
    }

    @Override
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        delegate.bind(endpoint, backlog);
    }

    @Override
    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    @Override
    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return delegate.getLocalSocketAddress();
    }

    @Override
    public Socket accept() throws IOException {
        return delegate.accept();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public ServerSocketChannel getChannel() {
        return delegate.getChannel();
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        delegate.setSoTimeout(timeout);
    }

    @Override
    public int getSoTimeout() throws IOException {
        return delegate.getSoTimeout();
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        delegate.setReuseAddress(on);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return delegate.getReuseAddress();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        delegate.setReceiveBufferSize(size);
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return delegate.getReceiveBufferSize();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    // TODO: evaluate other methods

}
