package io.sniffy.socket;

import io.sniffy.reflection.clazz.ClassRef;
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

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void sendUrgentData(int data) throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod("sendUrgentData", Integer.TYPE).invoke(delegate, data);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void shutdownInput() throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod("shutdownInput").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void shutdownOutput() throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod("shutdownOutput").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    protected FileDescriptor getFileDescriptor() {
        try {
            return socketImplClassRef.getNonStaticMethod(FileDescriptor.class, "getFileDescriptor").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    protected InetAddress getInetAddress() {
        try {
            return socketImplClassRef.getNonStaticMethod(InetAddress.class, "getInetAddress").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    protected int getPort() {
        try {
            return socketImplClassRef.getNonStaticMethod(Integer.TYPE, "getPort").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    protected boolean supportsUrgentData() {
        try {
            return socketImplClassRef.getNonStaticMethod(Boolean.TYPE, "supportsUrgentData").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    protected int getLocalPort() {
        try {
            return socketImplClassRef.getNonStaticMethod(Integer.TYPE, "getLocalPort").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    // TODO: wrap equals and hashCode methods

    @Override
    protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        try {
            socketImplClassRef.getNonStaticMethod(Integer.TYPE, "setPerformancePreferences", Integer.TYPE, Integer.TYPE, Integer.TYPE).invoke(delegate, connectionTime, latency, bandwidth);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void create(boolean stream) throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod(Integer.TYPE, "create", Boolean.TYPE).invoke(delegate, stream);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void connect(String host, int port) throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod(Integer.TYPE, "connect", String.class, Integer.TYPE).invoke(delegate, host, port);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod(Integer.TYPE, "connect", InetAddress.class, Integer.TYPE).invoke(delegate, address, port);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod(Integer.TYPE, "connect", SocketAddress.class, Integer.TYPE).invoke(delegate, address, timeout);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod(Integer.TYPE, "bind", InetAddress.class, Integer.TYPE).invoke(delegate, host, port);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void listen(int backlog) throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod(Integer.TYPE, "listen", Integer.TYPE).invoke(delegate, backlog);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void accept(SocketImpl s) throws IOException {
        try {
            /*
             * After Sniffy supports ServerSockets, we might need to add call to delegate.reset() here
             * And in general check the implementation of ServerSocket.implAccept(Socket s) method
             * for side effects
             *
             * s.reset();
             * s.fd = new FileDescriptor();
             * s.address = new InetAddress();
             *
             * Also consider implementing PlatformSocket interface
             */
            socketImplClassRef.getNonStaticMethod(Integer.TYPE, "accept", SocketImpl.class).invoke(delegate, s);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected InputStream getInputStream() throws IOException {
        try {
            return socketImplClassRef.getNonStaticMethod(InputStream.class, "getInputStream").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected OutputStream getOutputStream() throws IOException {
        try {
            return socketImplClassRef.getNonStaticMethod(OutputStream.class, "getOutputStream").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected int available() throws IOException {
        try {
            return socketImplClassRef.getNonStaticMethod(Integer.TYPE, "available").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void close() throws IOException {
        try {
            socketImplClassRef.getNonStaticMethod("close").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
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
