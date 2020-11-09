package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionCopier;
import io.sniffy.util.ReflectionUtil;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

import static io.sniffy.util.ReflectionUtil.invokeMethod;
import static io.sniffy.util.ReflectionUtil.setField;

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

                Object delegateCloseLock = ReflectionUtil.getField(AbstractInterruptibleChannel.class, delegate, "closeLock");

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (delegateCloseLock) {
                    setField(AbstractInterruptibleChannel.class, delegate, "closed", true);
                    invokeMethod(AbstractSelectableChannel.class, delegate, "implCloseSelectableChannel", Void.class);
                }

            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            }
        }

        @Override
        public void implConfigureBlocking(boolean block) {
            try {

                Object delegateRegLock = ReflectionUtil.getField(AbstractSelectableChannel.class, delegate, "regLock");

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (delegateRegLock) {
                    invokeMethod(AbstractSelectableChannel.class, delegate, "implConfigureBlocking", Boolean.TYPE, block, Void.class);
                    if (!setField(AbstractSelectableChannel.class, delegate, "nonBlocking", !block)) {
                        setField(AbstractSelectableChannel.class, delegate, "blocking", block); // Java 10 had blocking field instead of nonBlocking
                    }
                }

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

        // Note: this method is absent in newer JDKs so we cannot use @Override annotation
        // @Override
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
        public void park(int event, long nanos) throws IOException {
            try {
                invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Long.TYPE, nanos, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public void park(int event) throws IOException {
            try {
                invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

    }

    @SuppressWarnings("RedundantThrows")
    public static class SniffySinkChannel extends SinkChannel implements SelChImpl {

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

                Object delegateCloseLock = ReflectionUtil.getField(AbstractInterruptibleChannel.class, delegate, "closeLock");

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (delegateCloseLock) {
                    setField(AbstractInterruptibleChannel.class, delegate, "closed", true);
                    invokeMethod(AbstractSelectableChannel.class, delegate, "implCloseSelectableChannel", Void.class);
                }

            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            }
        }

        @Override
        public void implConfigureBlocking(boolean block) {
            try {

                Object delegateRegLock = ReflectionUtil.getField(AbstractSelectableChannel.class, delegate, "regLock");

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (delegateRegLock) {
                    invokeMethod(AbstractSelectableChannel.class, delegate, "implConfigureBlocking", Boolean.TYPE, block, Void.class);
                    if (!setField(AbstractSelectableChannel.class, delegate, "nonBlocking", !block)) {
                        setField(AbstractSelectableChannel.class, delegate, "blocking", block); // Java 10 had blocking field instead of nonBlocking
                    }
                }

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

        // Note: this method is absent in newer JDKs so we cannot use @Override annotation
        // @Override
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
        public void park(int event, long nanos) throws IOException {
            try {
                invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Long.TYPE, nanos, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public void park(int event) throws IOException {
            try {
                invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            }
        }

    }

}
