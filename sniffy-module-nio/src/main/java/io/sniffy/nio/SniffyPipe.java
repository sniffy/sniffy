package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.Unsafe;
import io.sniffy.util.ExceptionUtil;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.spi.SelectorProvider;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1.7
 */
public class SniffyPipe extends Pipe {

    private final SelectorProvider selectorProvider;
    private final Pipe delegate;

    public SniffyPipe(SelectorProvider selectorProvider, Pipe delegate) {
        this.selectorProvider = selectorProvider;
        this.delegate = delegate;
    }

    @Override
    public SourceChannel source() {
        return new SniffySourceChannel(selectorProvider, delegate.source());
    }

    @Override
    public SinkChannel sink() {
        return new SniffySinkChannel(selectorProvider, delegate.sink());
    }

    @SuppressWarnings("RedundantThrows")
    public static class SniffySourceChannel extends SourceChannel implements SelChImpl {

        private static final Polyglog LOG = PolyglogFactory.log(SniffySourceChannel.class);

        private final SourceChannel delegate;
        private final SelChImpl selChImplDelegate;

        public SniffySourceChannel(SelectorProvider provider, SourceChannel delegate) {
            super(provider);
            this.delegate = delegate;
            this.selChImplDelegate = (SelChImpl) delegate;
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

        // Note: this method is absent in newer JDKs, so we cannot use @Override annotation
        // @Override
        @SuppressWarnings("unused")
        public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
            try {
                $(SelChImpl.class).getNonStaticMethod("translateAndSetInterestOps", Integer.TYPE, SelectionKeyImpl.class).invoke(selChImplDelegate, ops, sk);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            }
        }

        // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
        //@Override
        @SuppressWarnings("unused")
        public int translateInterestOps(int ops) {
            try {
                return $(SelChImpl.class).getNonStaticMethod(Integer.TYPE, "translateInterestOps", Integer.TYPE).invoke(selChImplDelegate, ops);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            }
        }

        // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
        //@Override
        @SuppressWarnings("unused")
        public void park(int event, long nanos) throws IOException {
            try {
                $(SelChImpl.class).getNonStaticMethod("park", Integer.TYPE, Long.TYPE).invoke(selChImplDelegate, event, nanos);
            } catch (Exception e) {
                throw Unsafe.throwException(e);
            }
        }

        // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
        //@Override
        @SuppressWarnings("unused")
        public void park(int event) throws IOException {
            try {
                $(SelChImpl.class).getNonStaticMethod("park", Integer.TYPE).invoke(selChImplDelegate, event);
            } catch (Exception e) {
                throw Unsafe.throwException(e);
            }
        }

    }

    @SuppressWarnings("RedundantThrows")
    public static class SniffySinkChannel extends SinkChannel implements SelChImpl {

        private static final Polyglog LOG = PolyglogFactory.log(SniffySinkChannel.class);

        private final SinkChannel delegate;
        private final SelChImpl selChImplDelegate;

        public SniffySinkChannel(SelectorProvider provider, SinkChannel delegate) {
            super(provider);
            this.delegate = delegate;
            this.selChImplDelegate = (SelChImpl) delegate;
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

        // Note: this method is absent in newer JDKs, so we cannot use @Override annotation
        // @Override
        @SuppressWarnings("unused")
        public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
            try {
                $(SelChImpl.class).getNonStaticMethod("translateAndSetInterestOps", Integer.TYPE, SelectionKeyImpl.class).invoke(selChImplDelegate, ops, sk);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            }
        }

        // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
        //@Override
        @SuppressWarnings("unused")
        public int translateInterestOps(int ops) {
            try {
                return $(SelChImpl.class).getNonStaticMethod(Integer.TYPE, "translateInterestOps", Integer.TYPE).invoke(selChImplDelegate, ops);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            }
        }

        // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
        //@Override
        @SuppressWarnings("unused")
        public void park(int event, long nanos) throws IOException {
            try {
                $(SelChImpl.class).getNonStaticMethod("park", Integer.TYPE, Long.TYPE).invoke(selChImplDelegate, event, nanos);
            } catch (Exception e) {
                throw Unsafe.throwException(e);
            }
        }

        // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
        //@Override
        @SuppressWarnings("unused")
        public void park(int event) throws IOException {
            try {
                $(SelChImpl.class).getNonStaticMethod("park", Integer.TYPE).invoke(selChImplDelegate, event);
            } catch (Exception e) {
                throw Unsafe.throwException(e);
            }
        }

    }

}
