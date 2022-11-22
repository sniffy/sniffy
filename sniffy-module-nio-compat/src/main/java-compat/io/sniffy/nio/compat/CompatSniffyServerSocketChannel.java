package io.sniffy.nio.compat;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.nio.NioDelegateHelper;
import io.sniffy.nio.SelectableChannelWrapper;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.OSUtil;
import io.sniffy.util.StackTraceExtractor;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.ServerSocketChannelDelegate;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * @since 3.1.7
 */
// TODO: test properly and come up with a strategy for server sockets and server channels
public class CompatSniffyServerSocketChannel extends ServerSocketChannelDelegate implements SelectableChannelWrapper<ServerSocketChannel> {

    private static final Polyglog LOG = PolyglogFactory.log(CompatSniffyServerSocketChannel.class);

    private final ServerSocketChannel delegate;

    public CompatSniffyServerSocketChannel(SelectorProvider provider, ServerSocketChannel delegate) {
        super(provider, delegate);
        this.delegate = delegate;
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
        try {
            return new CompatSniffyServerSocket(delegate.socket(), this);
        } catch (IOException e) {
            // LOG.error(e); // TODO: uncomment
            return delegate.socket();
        }
    }

    @Override
    public SocketChannel accept() throws IOException {

        SocketChannel socketChannel = delegate.accept();

        if (null == socketChannel) {
            return null;
        }

        // Windows Selector is implemented using a pair of sockets which are explicitly cast and do not work with Sniffy
        // TODO: come up with something better
        return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                socketChannel :
                new CompatSniffySocketChannelAdapter(provider(), socketChannel);
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getLocalAddress() throws IOException {
        return delegate.getLocalAddress();
    }

    @Override
    public void implCloseSelectableChannel() {
        NioDelegateHelper.implCloseSelectableChannel(delegate);
    }

    @Override
    public void implConfigureBlocking(boolean block) {
        try {
            delegate.configureBlocking(block);
        } catch (Exception e) {
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

}
