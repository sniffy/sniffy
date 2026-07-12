package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionCopier;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.spi.SelectorProvider;


/**
 * @since 3.1.7
 */
public class SniffyPipe extends Pipe {

    private final SourceChannel source;
    private final SinkChannel sink;

    public SniffyPipe(SelectorProvider selectorProvider, Pipe delegate) {
        SniffySelectorProvider.assertOriginalPipe(delegate, "SniffyPipe");
        this.source = new SniffySourceChannel(selectorProvider, delegate.source());
        this.sink = new SniffySinkChannel(selectorProvider, delegate.sink());
    }

    @Override
    public SourceChannel source() {
        return source;
    }

    @Override
    public SinkChannel sink() {
        return sink;
    }

    @SuppressWarnings("RedundantThrows")
    public static class SniffySourceChannel extends SourceChannel implements SelChImpl, SelectableChannelWrapper<SourceChannel> {

        private final SourceChannel delegate;
        private final SelChImpl selChImplDelegate;
        private final ChannelRegistrationSupport registrationSupport = new ChannelRegistrationSupport();

        public SniffySourceChannel(SelectorProvider provider, SourceChannel delegate) {
            super(provider);
            SniffySelectorProvider.assertOriginalChannel(delegate, "SniffyPipe.SniffySourceChannel");
            this.delegate = delegate;
            this.selChImplDelegate = (SelChImpl) delegate;
        }

        @Override
        public SourceChannel getDelegate() {
            return delegate;
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
        public int read(ByteBuffer dst) throws IOException {
            return delegate.read(dst);
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            return delegate.read(dsts, offset, length);
        }

        @Override
        public long read(ByteBuffer[] dsts) throws IOException {
            return delegate.read(dsts);
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
        public void park(int event, long nanos) throws IOException {
            try {
                JdkNioAccess.resolve().park(selChImplDelegate, event, nanos);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public void park(int event) throws IOException {
            try {
                JdkNioAccess.resolve().park(selChImplDelegate, event);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

        @Override
        public void registerKeyLink(SelectionKeyLink link) {
            registrationSupport.register(link);
        }

        @Override
        public void unregisterKeyLink(SelectionKeyLink link) {
            registrationSupport.unregister(link);
        }

    }

    @SuppressWarnings("RedundantThrows")
    public static class SniffySinkChannel extends SinkChannel implements SelChImpl, SelectableChannelWrapper<SinkChannel> {

        private final SinkChannel delegate;
        private final SelChImpl selChImplDelegate;
        private final ChannelRegistrationSupport registrationSupport = new ChannelRegistrationSupport();

        public SniffySinkChannel(SelectorProvider provider, SinkChannel delegate) {
            super(provider);
            SniffySelectorProvider.assertOriginalChannel(delegate, "SniffyPipe.SniffySinkChannel");
            this.delegate = delegate;
            this.selChImplDelegate = (SelChImpl) delegate;
        }

        @Override
        public SinkChannel getDelegate() {
            return delegate;
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
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            return delegate.write(srcs, offset, length);
        }

        @Override
        public long write(ByteBuffer[] srcs) throws IOException {
            return delegate.write(srcs);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return delegate.write(src);
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
        public void park(int event, long nanos) throws IOException {
            try {
                JdkNioAccess.resolve().park(selChImplDelegate, event, nanos);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public void park(int event) throws IOException {
            try {
                JdkNioAccess.resolve().park(selChImplDelegate, event);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

        @Override
        public void registerKeyLink(SelectionKeyLink link) {
            registrationSupport.register(link);
        }

        @Override
        public void unregisterKeyLink(SelectionKeyLink link) {
            registrationSupport.unregister(link);
        }

    }

}
