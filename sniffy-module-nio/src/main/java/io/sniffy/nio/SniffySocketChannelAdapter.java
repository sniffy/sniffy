package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.ClassRef;
import io.sniffy.util.AssertUtil;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.StackTraceExtractor;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

import static io.sniffy.reflection.Unsafe.$;
import static io.sniffy.util.ReflectionUtil.*;

/**
 * @since 3.1.7
 */
public class SniffySocketChannelAdapter extends SocketChannel implements SelectableChannelWrapper<SocketChannel>, SelChImpl {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySocketChannelAdapter.class);

    private final SocketChannel delegate;
    private final SelChImpl selChImplDelegate;

    protected SniffySocketChannelAdapter(SelectorProvider provider, SocketChannel delegate) {
        super(provider);
        this.delegate = delegate;
        this.selChImplDelegate = (SelChImpl) delegate;
    }

    @Override
    public SocketChannel getDelegate() {
        return delegate;
    }

    @Override
    public AbstractSelectableChannel asSelectableChannel() {
        return this;
    }

    @SuppressWarnings({"Since15", "RedundantSuppression"})
    @Override
    @IgnoreJRERequirement
    public SocketChannel bind(SocketAddress local) throws IOException {
        delegate.bind(local);
        return this;
    }

    @SuppressWarnings({"Since15", "RedundantSuppression"})
    @Override
    @IgnoreJRERequirement
    public <T> SocketChannel setOption(java.net.SocketOption<T> name, T value) throws IOException {
        delegate.setOption(name, value);
        return this;
    }

    @SuppressWarnings({"Since15", "RedundantSuppression"})
    @Override
    @IgnoreJRERequirement
    public SocketChannel shutdownInput() throws IOException {
        delegate.shutdownInput();
        return this;
    }

    @SuppressWarnings({"Since15", "RedundantSuppression"})
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

    @SuppressWarnings({"Since15", "RedundantSuppression"})
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

    @Override
    public void implCloseSelectableChannel() {
        try {

            //Object delegateCloseLock = getField(AbstractInterruptibleChannel.class, delegate, "closeLock");

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
            }

            // todo: shall we copy keys from delegate to sniffy here ?

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
            LOG.error(e);
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    @IgnoreJRERequirement
    @SuppressWarnings({"Since15", "RedundantSuppression"})
    public <T> T getOption(java.net.SocketOption<T> name) throws IOException {
        return delegate.getOption(name);
    }

    @SuppressWarnings({"Since15", "RedundantSuppression"})
    @Override
    @IgnoreJRERequirement
    public Set<java.net.SocketOption<?>> supportedOptions() {
        return delegate.supportedOptions();
    }

    // Modern SelChImpl

    @Override
    public FileDescriptor getFD() {

        if (StackTraceExtractor.hasClassAndMethodInStackTrace("sun.nio.ch.FileChannelImpl", "transferToDirectly")) {
            return null; // disable zero-copy in order to intercept traffic
            // TODO: investigate enabling zero-copy but keeping traffic capture
        } else {
            return selChImplDelegate.getFD();
        }

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
    @SuppressWarnings("unused")
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
