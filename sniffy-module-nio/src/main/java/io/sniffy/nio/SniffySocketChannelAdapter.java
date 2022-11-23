package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.clazz.ClassRef;
import io.sniffy.reflection.method.UnresolvedNonStaticMethodRef;
import io.sniffy.reflection.method.UnresolvedNonStaticNonVoidMethodRef;
import io.sniffy.util.ExceptionUtil;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1.7
 */
public class SniffySocketChannelAdapter extends SocketChannelWrapper implements SelectableChannelWrapper<SocketChannel>, SelChImpl {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySocketChannelAdapter.class);
    public static final ClassRef<SelChImpl> SEL_CH_CLASS_REF = $(SelChImpl.class);
    public static final UnresolvedNonStaticMethodRef<SelChImpl> PARK =
            SEL_CH_CLASS_REF.getNonStaticMethod("park", Integer.TYPE);
    public static final UnresolvedNonStaticMethodRef<SelChImpl> PARK_NANOS =
            SEL_CH_CLASS_REF.getNonStaticMethod("park", Integer.TYPE, Long.TYPE);
    public static final UnresolvedNonStaticNonVoidMethodRef<SelChImpl, Integer> TRANSLATE_INTEREST_OPS =
            SEL_CH_CLASS_REF.getNonStaticMethod(Integer.TYPE, "translateInterestOps", Integer.TYPE);
    public static final UnresolvedNonStaticMethodRef<SelChImpl> TRANSLATE_AND_SET_INTEREST_OPS =
            SEL_CH_CLASS_REF.getNonStaticMethod("translateAndSetInterestOps", Integer.TYPE, SelectionKeyImpl.class);

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
            TRANSLATE_AND_SET_INTEREST_OPS.invoke(selChImplDelegate, ops, sk);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("unused")
    public int translateInterestOps(int ops) {
        try {
            return TRANSLATE_INTEREST_OPS.invoke(selChImplDelegate, ops);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "unused"})
    public void park(int event, long nanos) throws IOException {
        try {
            PARK_NANOS.invoke(selChImplDelegate, event, nanos);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "unused"})
    public void park(int event) throws IOException {
        try {
            PARK.invoke(selChImplDelegate, event);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

}
