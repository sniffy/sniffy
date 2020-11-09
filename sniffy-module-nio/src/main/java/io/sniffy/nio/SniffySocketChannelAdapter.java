package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionCopier;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

public class SniffySocketChannelAdapter extends SocketChannel implements SelChImpl {

    private static final ReflectionCopier<SocketChannel> socketChannelFieldsCopier = new ReflectionCopier<SocketChannel>(SocketChannel.class, "provider");

    protected final SocketChannel delegate;
    private final SelChImpl selChImplDelegate;

    protected SniffySocketChannelAdapter(SelectorProvider provider, SocketChannel delegate) {
        super(provider);
        this.delegate = delegate;
        this.selChImplDelegate = (SelChImpl) delegate;
    }

    @SuppressWarnings("Since15")
    @Override
    @IgnoreJRERequirement
    public SocketChannel bind(SocketAddress local) throws IOException {
        try {
            copyToDelegate();
            delegate.bind(local);
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("Since15")
    @Override
    @IgnoreJRERequirement
    public <T> SocketChannel setOption(java.net.SocketOption<T> name, T value) throws IOException {
        try {
            copyToDelegate();
            delegate.setOption(name, value);
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("Since15")
    @Override
    @IgnoreJRERequirement
    public SocketChannel shutdownInput() throws IOException {
        try {
            copyToDelegate();
            delegate.shutdownInput();
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("Since15")
    @Override
    @IgnoreJRERequirement
    public SocketChannel shutdownOutput() throws IOException {
        try {
            copyToDelegate();
            delegate.shutdownOutput();
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public Socket socket() {
        try {
            copyToDelegate();
            return delegate.socket(); // TODO: should we wrap it with SniffySocket ??
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean isConnected() {
        try {
            copyToDelegate();
            return delegate.isConnected();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean isConnectionPending() {
        try {
            copyToDelegate();
            return delegate.isConnectionPending();
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        copyToDelegate();
        try {
            return delegate.connect(remote);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean finishConnect() throws IOException {
        try {
            copyToDelegate();
            return delegate.finishConnect();
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("Since15")
    @Override
    @IgnoreJRERequirement
    public SocketAddress getRemoteAddress() throws IOException {
        try {
            copyToDelegate();
            return delegate.getRemoteAddress();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        try {
            copyToDelegate();
            return delegate.read(dst);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        try {
            copyToDelegate();
            return delegate.read(dsts, offset, length);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        try {
            copyToDelegate();
            return delegate.write(src);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        try {
            copyToDelegate();
            return delegate.write(srcs, offset, length);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        try {
            copyToDelegate();
            return delegate.getLocalAddress();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public void implCloseSelectableChannel() {
        try {
            copyToDelegate();
            invokeMethod(AbstractSelectableChannel.class, delegate, "implCloseSelectableChannel", Void.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public void implConfigureBlocking(boolean block) {
        try {
            copyToDelegate();
            invokeMethod(AbstractSelectableChannel.class, delegate, "implConfigureBlocking", Boolean.TYPE, block, Void.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public <T> T getOption(java.net.SocketOption<T> name) throws IOException {
        try {
            copyToDelegate();
            return delegate.getOption(name);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @SuppressWarnings("Since15")
    @Override
    @IgnoreJRERequirement
    public Set<java.net.SocketOption<?>> supportedOptions() {
        try {
            copyToDelegate();
            return delegate.supportedOptions();
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Modern SelChImpl

    @Override
    public FileDescriptor getFD() {
        try {
            copyToDelegate();
            return selChImplDelegate.getFD();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public int getFDVal() {
        try {
            copyToDelegate();
            return selChImplDelegate.getFDVal();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        try {
            copyToDelegate();
            return selChImplDelegate.translateAndUpdateReadyOps(ops, ski);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        try {
            copyToDelegate();
            return selChImplDelegate.translateAndSetReadyOps(ops, ski);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public void kill() throws IOException {
        try {
            copyToDelegate();
            selChImplDelegate.kill();
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method is absent in newer JDKs so we cannot use @Override annotation
    // @Override
    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        try {
            copyToDelegate();
            invokeMethod(SelChImpl.class, selChImplDelegate, "translateAndSetInterestOps", Integer.TYPE, ops, SelectionKeyImpl.class, sk, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public int translateInterestOps(int ops) {
        try {
            copyToDelegate();
            return invokeMethod(SelChImpl.class, selChImplDelegate, "translateInterestOps", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public void park(int event, long nanos) throws IOException {
        try {
            copyToDelegate();
            invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Long.TYPE, nanos, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public void park(int event) throws IOException {
        try {
            copyToDelegate();
            invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // TODO: think about thread safety - shall we make everything synschronized and made more stable but less performant ?
    protected void copyToDelegate() {
        socketChannelFieldsCopier.copy(this, delegate);
    }

    // TODO: think about thread safety
    protected void copyFromDelegate() {
        socketChannelFieldsCopier.copy(delegate, this);
    }

}
