package io.sniffy.socket;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionCopier;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.util.Set;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * @since 3.1.7
 */
class SniffySocketImplAdapter extends SocketImpl {

    private static final ReflectionCopier<SocketImpl> socketChannelFieldsCopier = new ReflectionCopier<SocketImpl>(SocketImpl.class);

    protected final SocketImpl delegate;

    protected SniffySocketImplAdapter(SocketImpl delegate) {
        this.delegate = delegate;
    }

    private void copyToDelegate() {
        socketChannelFieldsCopier.copy(this, delegate);
    }

    private void copyFromDelegate() {
        socketChannelFieldsCopier.copy(delegate, this);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void sendUrgentData(int data) throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "sendUrgentData", int.class, data, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void shutdownInput() throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "shutdownInput", Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void shutdownOutput() throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "shutdownOutput", Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    protected FileDescriptor getFileDescriptor() {
        copyToDelegate();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getFileDescriptor", FileDescriptor.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    protected InetAddress getInetAddress() {
        copyToDelegate();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getInetAddress", InetAddress.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    protected int getPort() {
        copyToDelegate();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getPort", Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    protected boolean supportsUrgentData() {
        copyToDelegate();
        try {
            return invokeMethod(SocketImpl.class, delegate, "supportsUrgentData", Boolean.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    protected int getLocalPort() {
        copyToDelegate();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getLocalPort", Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public String toString() {
        copyToDelegate();
        try {
            return delegate.toString();
        } finally {
            copyFromDelegate();
        }
    }

    // TODO: wrap equals and hashCode methods

    @Override
    protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "setPerformancePreferences", Integer.TYPE, connectionTime, Integer.TYPE, latency, Integer.TYPE, bandwidth, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void create(boolean stream) throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "create", Boolean.TYPE, stream, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void connect(String host, int port) throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "connect", String.class, host, Integer.TYPE, port, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "connect", InetAddress.class, address, Integer.TYPE, port, Void.TYPE);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "connect", SocketAddress.class, address, Integer.TYPE, timeout, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "bind", InetAddress.class, host, Integer.TYPE, port, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void listen(int backlog) throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "listen", Integer.TYPE, backlog, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void accept(SocketImpl s) throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "accept", SocketImpl.class, s, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected InputStream getInputStream() throws IOException {
        copyToDelegate();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getInputStream", InputStream.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected OutputStream getOutputStream() throws IOException {
        copyToDelegate();
        try {
            return invokeMethod(SocketImpl.class, delegate, "getOutputStream", OutputStream.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected int available() throws IOException {
        copyToDelegate();
        try {
            return invokeMethod(SocketImpl.class, delegate, "available", Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void close() throws IOException {
        copyToDelegate();
        try {
            invokeMethod(SocketImpl.class, delegate, "close", Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // interface

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        copyToDelegate();
        try {
            delegate.setOption(optID, value);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        copyToDelegate();
        try {
            return delegate.getOption(optID);
        } finally {
            copyFromDelegate();
        }
    }

    // New methods in Java 9

    @Override
    @IgnoreJRERequirement
    protected <T> void setOption(java.net.SocketOption<T> name, T value) throws IOException {
        try {
            copyToDelegate();
            invokeMethod(SocketImpl.class, delegate, "setOption", java.net.SocketOption.class, name, Object.class, value, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    protected <T> T getOption(java.net.SocketOption<T> name) throws IOException {
        try {
            copyToDelegate();
            //noinspection unchecked
            return (T) invokeMethod(SocketImpl.class, delegate, "getOption", java.net.SocketOption.class, name, Object.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    protected Set<java.net.SocketOption<?>> supportedOptions() {
        try {
            copyToDelegate();
            //noinspection unchecked
            return (Set<java.net.SocketOption<?>>) invokeMethod(SocketImpl.class, delegate, "supportedOptions", Set.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }
}
