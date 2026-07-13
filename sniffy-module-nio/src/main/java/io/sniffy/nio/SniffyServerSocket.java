package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/** ServerSocket view which keeps all lifecycle and accept operations on the Sniffy channel. */
final class SniffyServerSocket extends ServerSocket {

    private final ServerSocket delegate;
    private final SniffyServerSocketChannel channel;

    SniffyServerSocket(ServerSocket delegate, SniffyServerSocketChannel channel) throws IOException {
        super();
        this.delegate = delegate;
        this.channel = channel;
    }

    @Override
    public void bind(SocketAddress endpoint) throws IOException {
        channel.bind(endpoint);
    }

    @Override
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        channel.bind(endpoint, backlog);
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
        if (!channel.isBlocking()) throw new IllegalBlockingModeException();
        return channel.acceptSocket();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public ServerSocketChannel getChannel() {
        return channel;
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public boolean isClosed() {
        return !channel.isOpen();
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

    @IgnoreJRERequirement
    public <T> ServerSocket setOption(SocketOption<T> name, T value) throws IOException {
        try {
            invokeMethod(ServerSocket.class, delegate, "setOption",
                    SocketOption.class, name, Object.class, value, ServerSocket.class);
            return this;
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @IgnoreJRERequirement
    @SuppressWarnings("unchecked")
    public <T> T getOption(SocketOption<T> name) throws IOException {
        try {
            return (T) invokeMethod(ServerSocket.class, delegate, "getOption",
                    SocketOption.class, name, Object.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @IgnoreJRERequirement
    @SuppressWarnings("unchecked")
    public Set<SocketOption<?>> supportedOptions() {
        try {
            return (Set<SocketOption<?>>) invokeMethod(ServerSocket.class, delegate, "supportedOptions", Set.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
