package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;


/**
 * @since 3.1.7
 */
public class SniffyServerSocketChannel extends ServerSocketChannel implements SelChImpl, SelectableChannelWrapper<ServerSocketChannel> {

    private final ServerSocketChannel delegate;
    private final SelChImpl selChImplDelegate;
    private final ServerSocket socket;

    public SniffyServerSocketChannel(SelectorProvider provider, ServerSocketChannel delegate) throws IOException {
        super(provider);
        SniffySelectorProvider.assertOriginalChannel(delegate, "SniffyServerSocketChannel");
        this.delegate = delegate;
        this.selChImplDelegate = (SelChImpl) delegate;
        this.socket = new SniffyServerSocket(delegate.socket(), this);
    }

    @Override
    public ServerSocketChannel getDelegate() {
        return delegate;
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

    Socket acceptSocket() throws IOException {
        // ServerSocketAdaptor implements SO_TIMEOUT without changing channel-level accept.
        // No selector or channel lifecycle monitor is held while this call can block.
        Socket acceptedSocket = delegate.socket().accept();
        SocketChannel acceptedChannel = acceptedSocket.getChannel();
        if (acceptedChannel == null) {
            acceptedSocket.close();
            throw new IOException("Accepted socket did not expose its SocketChannel");
        }
        try {
            return new SniffySocketChannel(provider(), acceptedChannel).socket();
        } catch (Throwable failure) {
            try {
                acceptedSocket.close();
            } catch (Throwable cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw ExceptionUtil.throwException(failure);
        }
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getLocalAddress() throws IOException {
        return delegate.getLocalAddress();
    }

    @Override
    public void implCloseSelectableChannel() throws IOException {
        delegate.close();
    }

    @Override
    public void implConfigureBlocking(boolean block) throws IOException {
        delegate.configureBlocking(block);
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
            JdkNioAccess.resolve().translateAndSetInterestOps(selChImplDelegate, ops, sk);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public int translateInterestOps(int ops) {
        try {
            return JdkNioAccess.resolve().translateInterestOps(selChImplDelegate, ops);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public void park(int event, long nanos) throws IOException {
        try {
            JdkNioAccess.resolve().park(selChImplDelegate, event, nanos);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public void park(int event) throws IOException {
        try {
            JdkNioAccess.resolve().park(selChImplDelegate, event);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

}
