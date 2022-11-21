package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.AssertUtil;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.OSUtil;
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
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1.7
 */
// TODO: test properly and come up with a strategy for server sockets and server channels
public class SniffyServerSocketChannel extends ServerSocketChannel implements SelChImpl, SelectableChannelWrapper<ServerSocketChannel> {

    private static final Polyglog LOG = PolyglogFactory.log(SniffyServerSocketChannel.class);

    private final ServerSocketChannel delegate;
    private final SelChImpl selChImplDelegate;

    public SniffyServerSocketChannel(SelectorProvider provider, ServerSocketChannel delegate) {
        super(provider);
        this.delegate = delegate;
        this.selChImplDelegate = (SelChImpl) delegate;
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
            return new SniffyServerSocket(delegate.socket(), this);
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
                new SniffySocketChannelAdapter(provider(), socketChannel);
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getLocalAddress() throws IOException {
        return delegate.getLocalAddress();
    }

    @Override
    public void implCloseSelectableChannel() {
        try {

            // TODO: extract code below so it could be reused
            boolean changed = false;

            synchronized ($(AbstractInterruptibleChannel.class).field("closedLock").getNotNullOrDefault(delegate, delegate)) {

                if ($(AbstractInterruptibleChannel.class).field("closed").isResolved()) {
                    changed = $(AbstractInterruptibleChannel.class).field("closed").compareAndSet(delegate, false, true);
                } else {
                    if ($(AbstractInterruptibleChannel.class).field("open").isResolved()) {
                        changed = $(AbstractInterruptibleChannel.class).field("open").compareAndSet(delegate, true, false);
                    } else {
                        AssertUtil.logAndThrowException(LOG, "Couldn't find neither closed nor open field in AbstractInterruptibleChannel", new IllegalStateException());
                    }
                }

            }

            if (changed) {
                $(AbstractSelectableChannel.class).method("implCloseSelectableChannel").invoke(delegate); // or selectable
            } else {
                if (AssertUtil.isTestingSniffy()) {
                    if ($(AbstractInterruptibleChannel.class).field("closed").isResolved()) {
                        if (!$(AbstractInterruptibleChannel.class).<Boolean>field("closed").get(delegate)) {
                            AssertUtil.logAndThrowException(LOG, "Failed to close delegate selector", new IllegalStateException());
                        }
                    } else {
                        if ($(AbstractInterruptibleChannel.class).field("open").isResolved()) {
                            if ($(AbstractInterruptibleChannel.class).<Boolean>field("open").get(delegate)) {
                                AssertUtil.logAndThrowException(LOG, "Failed to close delegate selector", new IllegalStateException());
                            }
                        } else {
                            AssertUtil.logAndThrowException(LOG, "Couldn't find neither closed nor open field in AbstractInterruptibleChannel", new IllegalStateException());
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.processException(e);
        }
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

    // Note: this method is absent in newer JDKs, so we cannot use @Override annotation
    // @Override
    @SuppressWarnings("unused")
    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        try {
            $(SelChImpl.class).method("translateAndSetInterestOps", Integer.TYPE, SelectionKeyImpl.class).invoke(selChImplDelegate, ops, sk);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
    //@Override
    public int translateInterestOps(int ops) {
        try {
            return $(SelChImpl.class).method(Integer.TYPE, "translateInterestOps", Integer.TYPE).invoke(selChImplDelegate, ops);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public void park(int event, long nanos) throws IOException {
        try {
            $(SelChImpl.class).method("park", Integer.TYPE, Long.TYPE).invoke(selChImplDelegate, event, nanos);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public void park(int event) throws IOException {
        try {
            $(SelChImpl.class).method("park", Integer.TYPE).invoke(selChImplDelegate, event);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

}
