package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.AssertUtil;
import io.sniffy.util.ExceptionUtil;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
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
            try {

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
        public void park(int event, long nanos) throws IOException {
            try {
                $(SelChImpl.class).method("park", Integer.TYPE, Long.TYPE).invoke(selChImplDelegate, event, nanos);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

        // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
        //@Override
        public void park(int event) throws IOException {
            try {
                $(SelChImpl.class).method("park", Integer.TYPE).invoke(selChImplDelegate, event);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
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
            try {

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
        public void park(int event, long nanos) throws IOException {
            try {
                $(SelChImpl.class).method("park", Integer.TYPE, Long.TYPE).invoke(selChImplDelegate, event, nanos);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

        // Note: this method was absent in earlier JDKs, so we cannot use @Override annotation
        //@Override
        public void park(int event) throws IOException {
            try {
                $(SelChImpl.class).method("park", Integer.TYPE).invoke(selChImplDelegate, event);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

    }

}
