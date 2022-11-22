package io.sniffy.socket;

import io.sniffy.reflection.ClassRef;
import io.sniffy.util.ExceptionUtil;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1.9
 */
class CompatSniffySocketImplAdapter extends SocketImpl {

    private static final ClassRef<SocketImpl> socketImplClassRef = $(SocketImpl.class);

    protected final SocketImpl delegate;

    protected CompatSniffySocketImplAdapter(SocketImpl delegate) {
        this.delegate = delegate;
    }

    private void copyToDelegate() {
        socketImplClassRef.copyFields(this, delegate);
    }

    private void copyFromDelegate() {
        socketImplClassRef.copyFields(delegate, this);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void sendUrgentData(int data) throws IOException {
        copyToDelegate();
        try {
            $(SocketImpl.class).method("sendUrgentData", Integer.TYPE).invoke(delegate, data);
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
            $(SocketImpl.class).method("shutdownInput").invoke(delegate);
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
            $(SocketImpl.class).method("shutdownOutput").invoke(delegate);
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
            return $(SocketImpl.class).method(FileDescriptor.class, "getFileDescriptor").invoke(delegate);
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
            return $(SocketImpl.class).method(InetAddress.class, "getInetAddress").invoke(delegate);
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
            return $(SocketImpl.class).method(Integer.TYPE, "getPort").invoke(delegate);
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
            return $(SocketImpl.class).method(Boolean.TYPE, "supportsUrgentData").invoke(delegate);
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
            return $(SocketImpl.class).method(Integer.TYPE, "getLocalPort").invoke(delegate);
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
            $(SocketImpl.class).method(Integer.TYPE, "setPerformancePreferences", Integer.TYPE, Integer.TYPE, Integer.TYPE).invoke(delegate, connectionTime, latency, bandwidth);
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
            $(SocketImpl.class).method(Integer.TYPE, "create", Boolean.TYPE).invoke(delegate, stream);
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
            $(SocketImpl.class).method(Integer.TYPE, "connect", String.class, Integer.TYPE).invoke(delegate, host, port);
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
            $(SocketImpl.class).method(Integer.TYPE, "connect", InetAddress.class, Integer.TYPE).invoke(delegate, address, port);
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
            $(SocketImpl.class).method(Integer.TYPE, "connect", SocketAddress.class, Integer.TYPE).invoke(delegate, address, timeout);
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
            $(SocketImpl.class).method(Integer.TYPE, "bind", InetAddress.class, Integer.TYPE).invoke(delegate, host, port);
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
            $(SocketImpl.class).method(Integer.TYPE, "listen", Integer.TYPE).invoke(delegate, backlog);
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
            $(SocketImpl.class).method(Integer.TYPE, "accept", SocketImpl.class).invoke(delegate, s);
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
            return $(SocketImpl.class).method(InputStream.class, "getInputStream").invoke(delegate);
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
            return $(SocketImpl.class).method(OutputStream.class, "getOutputStream").invoke(delegate);
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
            return $(SocketImpl.class).method(Integer.TYPE, "available").invoke(delegate);
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
            $(SocketImpl.class).method("close").invoke(delegate);
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

    // TODO: methods below are commented out because Java 6 doesn't have SocketOption class
    /*

    // @Override - available in Java 9+ only
    @SuppressWarnings("Since15")
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

    // @Override - available in Java 9+ only
    @SuppressWarnings("Since15")
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

    // @Override - available in Java 9+ only
    @SuppressWarnings("Since15")
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

    */

}
