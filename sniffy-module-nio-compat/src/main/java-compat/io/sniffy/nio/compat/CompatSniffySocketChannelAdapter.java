package io.sniffy.nio.compat;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.nio.NioDelegateHelper;
import io.sniffy.nio.SelectableChannelWrapper;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.StackTraceExtractor;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.SocketChannelDelegate;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * @since 3.1.7
 */
public class CompatSniffySocketChannelAdapter extends SocketChannelDelegate implements SelectableChannelWrapper<SocketChannel> {

    private static final Polyglog LOG = PolyglogFactory.log(CompatSniffySocketChannelAdapter.class);

    protected final SocketChannel delegate;

    protected CompatSniffySocketChannelAdapter(SelectorProvider provider, SocketChannel delegate) {
        super(provider, delegate);
        this.delegate = delegate;
    }

    @Override
    public SocketChannel getDelegate() {
        return delegate;
    }

    @SuppressWarnings("Since17")
    @Override
    @IgnoreJRERequirement
    public SocketChannel bind(SocketAddress local) throws IOException {
        delegate.bind(local);
        return this;
    }

    @SuppressWarnings("Since17")
    @Override
    @IgnoreJRERequirement
    public <T> SocketChannel setOption(java.net.SocketOption<T> name, T value) throws IOException {
        delegate.setOption(name, value);
        return this;
    }

    @SuppressWarnings("Since17")
    @Override
    @IgnoreJRERequirement
    public SocketChannel shutdownInput() throws IOException {
        delegate.shutdownInput();
        return this;
    }

    @SuppressWarnings("Since17")
    @Override
    @IgnoreJRERequirement
    public SocketChannel shutdownOutput() throws IOException {
        delegate.shutdownOutput();
        return this;
    }

    @Override
    public Socket socket() {
        return delegate.socket();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean isConnectionPending() {
        return delegate.isConnectionPending();
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        return delegate.connect(remote);
    }

    @Override
    public boolean finishConnect() throws IOException {
        return delegate.finishConnect();
    }

    @SuppressWarnings("Since17")
    @Override
    @IgnoreJRERequirement
    public SocketAddress getRemoteAddress() throws IOException {
        return delegate.getRemoteAddress();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return delegate.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return delegate.read(dsts, offset, length);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return delegate.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return delegate.write(srcs, offset, length);
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return delegate.getLocalAddress();
    }

    /**
     * SniffySocketChannelAdapter extends SocketChannel
     * extends AbstractSelectableChannel extends SelectableChannel
     * extends AbstractInterruptibleChannel
     * final AbstractInterruptibleChannel.close() = if (!closed) { close = true; implCloseChannel() }
     * final AbstractSelectableChannel.implCloseChannel() = { implCloseSelectableChannel(); cancelAllKeysInChannel() }
     * Sniffy invokes implCloseSelectableChannel() on delegate, and after that cancels the keys from implCloseChannel()
     */
    @Override
    public void implCloseSelectableChannel() {
        NioDelegateHelper.implCloseSelectableChannel(delegate);
    }

    @Override
    public void implConfigureBlocking(boolean block) {
        try {
            delegate.configureBlocking(block);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    @IgnoreJRERequirement
    @SuppressWarnings({"Since17"})
    public <T> T getOption(java.net.SocketOption<T> name) throws IOException {
        return delegate.getOption(name);
    }

    @SuppressWarnings("Since17")
    @Override
    @IgnoreJRERequirement
    public Set<java.net.SocketOption<?>> supportedOptions() {
        return delegate.supportedOptions();
    }

    @Override
    public FileDescriptor getFD() {

        if (StackTraceExtractor.hasClassAndMethodInStackTrace("sun.nio.ch.FileChannelImpl", "transferToDirectly")) {
            return null; // disable zero-copy in order to intercept traffic
            // TODO: investigate enabling zero-copy but keeping traffic capture
        } else {
            return super.getFD();
        }

    }

}
