package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
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
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * @since 3.1.7
 */
public class SniffyServerSocketChannel extends ServerSocketChannel implements SelChImpl, SelectableChannelWrapper<ServerSocketChannel> {

    private final ServerSocketChannel delegate;
    private final SelChImpl selChImplDelegate;
    private final ServerSocket socket;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private volatile boolean hasCancelledKeys;

    public SniffyServerSocketChannel(SelectorProvider provider, ServerSocketChannel delegate) throws IOException {
        super(provider);
        this.delegate = delegate;
        this.selChImplDelegate = (SelChImpl) delegate;
        this.socket = new SniffyServerSocket(delegate.socket(), this);
    }

    @Override
    public ServerSocketChannel getDelegate() {
        return delegate;
    }

    @Override
    public void keyCancelled() {
        hasCancelledKeys = true;
    }

    @Override
    @IgnoreJRERequirement
    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        delegate.bind(local, backlog);
        return this;
    }

    @Override
    @IgnoreJRERequirement
    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        delegate.setOption(name, value);
        return this;
    }

    @Override
    public ServerSocket socket() {
        return socket;
    }

    @Override
    public SocketChannel accept() throws IOException {

        SocketChannel socketChannel = delegate.accept();

        if (null == socketChannel) {
            return null;
        }

        return new SniffySocketChannel(provider(), socketChannel);
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getLocalAddress() throws IOException {
        return delegate.getLocalAddress();
    }

    @Override
    public void implCloseSelectableChannel() {
        try {
            delegate.close();
        } catch (IOException e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    public void implConfigureBlocking(boolean block) {
        try {
            delegate.configureBlocking(block);
        } catch (IOException e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    @IgnoreJRERequirement
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return delegate.getOption(name);
    }

    @Override
    @IgnoreJRERequirement
    public Set<SocketOption<?>> supportedOptions() {
        return delegate.supportedOptions();
    }

    // Modern SelChImpl

    @Override
    public FileDescriptor getFD() {
        return selChImplDelegate.getFD();
    }

    @Override
    public int getFDVal() {
        return selChImplDelegate.getFDVal();
    }

    @Override
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        return selChImplDelegate.translateAndUpdateReadyOps(ops, ski);
    }

    @Override
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        return selChImplDelegate.translateAndSetReadyOps(ops, ski);
    }

    @Override
    public void kill() throws IOException {
        selChImplDelegate.kill();
    }

    // Note: this method is absent in newer JDKs so we cannot use @Override annotation
    // @Override
    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        try {
            invokeMethod(SelChImpl.class, selChImplDelegate, "translateAndSetInterestOps", Integer.TYPE, ops, SelectionKeyImpl.class, sk, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public int translateInterestOps(int ops) {
        try {
            return invokeMethod(SelChImpl.class, selChImplDelegate, "translateInterestOps", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public void park(int event, long nanos) throws IOException {
        try {
            invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Long.TYPE, nanos, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public void park(int event) throws IOException {
        try {
            invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

}
