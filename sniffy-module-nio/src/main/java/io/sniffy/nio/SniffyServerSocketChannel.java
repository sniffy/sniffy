package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.OSUtil;
import io.sniffy.util.ReflectionCopier;
import io.sniffy.util.StackTraceExtractor;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

public class SniffyServerSocketChannel extends ServerSocketChannel implements SelChImpl {

    private static final ReflectionCopier<ServerSocketChannel> socketChannelFieldsCopier = new ReflectionCopier<ServerSocketChannel>(ServerSocketChannel.class, "provider");

    protected final ServerSocketChannel delegate;
    private final SelChImpl selChImplDelegate;

    public SniffyServerSocketChannel(SelectorProvider provider, ServerSocketChannel delegate) {
        super(provider);
        this.delegate = delegate;
        this.selChImplDelegate = (SelChImpl) delegate;
    }

    private void copyToDelegate() {
        socketChannelFieldsCopier.copy(this, delegate);
    }

    private void copyFromDelegate() {
        socketChannelFieldsCopier.copy(delegate, this);
    }

    @Override
    @IgnoreJRERequirement
    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        try {
            copyToDelegate();
            delegate.bind(local, backlog);
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        try {
            copyToDelegate();
            delegate.setOption(name, value);
            return this;
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public ServerSocket socket() {
        try {
            copyToDelegate();
            return delegate.socket(); // TODO: should we wrap it with SniffyServerSocket ??
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public SocketChannel accept() throws IOException {
        try {
            copyToDelegate();
            return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                delegate.accept() :
                new SniffySocketChannelAdapter(provider(), delegate.accept()); // TODO: distinguish it from real client socket channels
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getLocalAddress() throws IOException {
        try {
            copyToDelegate();
            return delegate.getLocalAddress();
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public void implCloseSelectableChannel() {
        copyToDelegate();
        try {
            invokeMethod(AbstractSelectableChannel.class, delegate, "implCloseSelectableChannel", Void.class);
        } catch (Exception e) {
            ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public void implConfigureBlocking(boolean block) {
        copyToDelegate();
        try {
            invokeMethod(AbstractSelectableChannel.class, delegate, "implConfigureBlocking", Boolean.TYPE, block, Void.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public <T> T getOption(SocketOption<T> name) throws IOException {
        try {
            copyToDelegate();
            return delegate.getOption(name);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    @IgnoreJRERequirement
    public Set<SocketOption<?>> supportedOptions() {
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
        copyToDelegate();
        try {
            return selChImplDelegate.getFD();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public int getFDVal() {
        copyToDelegate();
        try {
            return selChImplDelegate.getFDVal();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        copyToDelegate();
        try {
            return selChImplDelegate.translateAndUpdateReadyOps(ops, ski);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        copyToDelegate();
        try {
            return selChImplDelegate.translateAndSetReadyOps(ops, ski);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public void kill() throws IOException {
        copyToDelegate();
        try {
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

}
