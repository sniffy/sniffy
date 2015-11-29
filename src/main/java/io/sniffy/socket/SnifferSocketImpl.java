package io.sniffy.socket;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

public class SnifferSocketImpl extends SocketImpl {

    private final SocketImpl delegate;

    public SnifferSocketImpl(SocketImpl delegate) {
        this.delegate = delegate;
    }

    private static Method method(String methodName, Class<?>... argumentTypes) throws NoSuchMethodException {
        Method method = SocketImpl.class.getDeclaredMethod(methodName, argumentTypes);
        method.setAccessible(true);
        return method;
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        try {
            method("sendUrgentData", int.class).invoke(delegate, data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void shutdownInput() throws IOException {
        try {
            method("shutdownInput").invoke(delegate);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void shutdownOutput() throws IOException {
        try {
            method("shutdownOutput").invoke(delegate);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected FileDescriptor getFileDescriptor() {
        try {
            return (FileDescriptor) method("getFileDescriptor").invoke(delegate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected InetAddress getInetAddress() {
        try {
            return (InetAddress) method("getInetAddress").invoke(delegate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int getPort() {
        try {
            return (Integer) method("getPort").invoke(delegate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean supportsUrgentData() {
        try {
            return (Boolean) method("supportsUrgentData").invoke(delegate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int getLocalPort() {
        try {
            return (Integer) method("getLocalPort").invoke(delegate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        try {
            method("setPerformancePreferences", int.class, int.class, int.class).invoke(delegate, connectionTime, latency, bandwidth);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void create(boolean stream) throws IOException {
        try {
            method("create", boolean.class).invoke(delegate, stream);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        try {
            method("connect", String.class, int.class).invoke(delegate, host, port);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        try {
            method("connect", InetAddress.class, int.class).invoke(delegate, address, port);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        try {
            method("connect", SocketAddress.class, int.class).invoke(delegate, address, timeout);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        try {
            method("bind", InetAddress.class, int.class).invoke(delegate, host, port);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void listen(int backlog) throws IOException {
        try {
            method("listen", int.class).invoke(delegate, backlog);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        try {
            method("accept", SocketImpl.class).invoke(delegate, s);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        try {
            return (InputStream) method("getInputStream").invoke(delegate);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        try {
            return (OutputStream) method("getOutputStream").invoke(delegate);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected int available() throws IOException {
        try {
            return (Integer) method("available").invoke(delegate);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void close() throws IOException {
        try {
            method("close").invoke(delegate);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    // interface

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        delegate.setOption(optID, value);
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        return delegate.getOption(optID);
    }
}
