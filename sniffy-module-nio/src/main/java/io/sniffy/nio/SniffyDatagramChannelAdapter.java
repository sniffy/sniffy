package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.ExceptionUtil;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1.7
 */
public class SniffyDatagramChannelAdapter extends DatagramChannel implements SelectableChannelWrapper<DatagramChannel>, SelChImpl {

    // TODO add proper tests for SniffyDatagramChannelAdapter as well
    private static final Polyglog LOG = PolyglogFactory.log(SniffyDatagramChannelAdapter.class);

    private final DatagramChannel delegate;
    private final SelChImpl selChImplDelegate;

    protected SniffyDatagramChannelAdapter(SelectorProvider provider, DatagramChannel delegate) {
        super(provider);
        this.delegate = delegate;
        this.selChImplDelegate = (SelChImpl) delegate;
    }

    @Override
    public DatagramChannel getDelegate() {
        return delegate;
    }

    @Override
    @IgnoreJRERequirement
    public DatagramChannel bind(SocketAddress local) throws IOException {
        delegate.bind(local);
        return this;
    }

    @Override
    @IgnoreJRERequirement
    public <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException {
        delegate.setOption(name, value);
        return this;
    }

    @Override
    public DatagramSocket socket() {
        return delegate.socket();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public DatagramChannel connect(SocketAddress remote) throws IOException {
        delegate.connect(remote);
        return this;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public DatagramChannel disconnect() throws IOException {
        delegate.disconnect();
        return this;
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getRemoteAddress() throws IOException {
        return delegate.getRemoteAddress();
    }

    @Override
    public SocketAddress receive(ByteBuffer dst) throws IOException {
        return delegate.receive(dst);
    }

    @Override
    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        return delegate.send(src, target);
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
    @IgnoreJRERequirement
    @SuppressWarnings("RedundantThrows")
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
    @SuppressWarnings("RedundantThrows")
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return delegate.getOption(name);
    }

    @Override
    @IgnoreJRERequirement
    public Set<SocketOption<?>> supportedOptions() {
        return delegate.supportedOptions();
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    @IgnoreJRERequirement
    public MembershipKey join(InetAddress group, NetworkInterface interf) throws IOException {
        return delegate.join(group, interf);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    @IgnoreJRERequirement
    public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException {
        return delegate.join(group, interf, source);
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
