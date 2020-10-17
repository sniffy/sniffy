package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionFieldCopier;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.List;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

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
        return null;
    }

    public static class SniffySourceChannel extends SourceChannel implements SelChImpl {

        private final SourceChannel delegate;
        private final SelChImpl selChImplDelegate;

        private final static ReflectionFieldCopier keysCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "keys");
        private final static ReflectionFieldCopier keyCountCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "keyCount");
        private final static ReflectionFieldCopier keyLockCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "keyLock");
        private final static ReflectionFieldCopier regLockCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "regLock");
        private final static ReflectionFieldCopier nonBlockingCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "nonBlocking");
        private final static ReflectionFieldCopier blockingCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "blocking");

        private final static ReflectionFieldCopier closeLockCopier =
                new ReflectionFieldCopier(AbstractInterruptibleChannel.class, "closeLock");
        private final static ReflectionFieldCopier closedCopier =
                new ReflectionFieldCopier(AbstractInterruptibleChannel.class, "closed");

        private static volatile ReflectionFieldCopier[] reflectionFieldCopiers;

        public SniffySourceChannel(SelectorProvider provider, SourceChannel delegate) {
            super(provider);
            this.delegate = delegate;
            this.selChImplDelegate = (SelChImpl) delegate;
        }

        @Override
        public void implCloseSelectableChannel() {
            copyToDelegate();
            try {
                invokeMethod(AbstractSelectableChannel.class, delegate, "implCloseSelectableChannel", Void.class);
            } catch (Exception e) {
                ExceptionUtil.processException(e);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public void implConfigureBlocking(boolean block) {
            copyToDelegate();
            try {
                invokeMethod(AbstractSelectableChannel.class, delegate, "implConfigureBlocking", Void.class);
            } catch (Exception e) {
                ExceptionUtil.processException(e);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            copyToDelegate();
            try {
                return delegate.read(dst);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            copyToDelegate();
            try {
                return delegate.read(dsts, offset, length);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public long read(ByteBuffer[] dsts) throws IOException {
            copyToDelegate();
            try {
                return delegate.read(dsts);
            } finally {
                copyFromDelegate();
            }
        }

        // Modern SelChImpl

        @Override
        public FileDescriptor getFD() {
            copyToDelegate();
            try {
                return selChImplDelegate.getFD();
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public int getFDVal() {
            copyToDelegate();
            try {
                return selChImplDelegate.getFDVal();
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
            copyToDelegate();
            try {
                return selChImplDelegate.translateAndUpdateReadyOps(ops, ski);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
            copyToDelegate();
            try {
                return selChImplDelegate.translateAndSetReadyOps(ops, ski);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public void kill() throws IOException {
            copyToDelegate();
            try {
                selChImplDelegate.kill();
            } finally {
                copyFromDelegate();
            }
        }

        // Note: this method is absent in newer JDKs so we cannot use @Override annotation
        // @Override
        public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
            try {
                copyToDelegate();
                invokeMethod(SelChImpl.class, selChImplDelegate, "translateAndSetInterestOps", Integer.TYPE, ops, SelectionKeyImpl.class, sk, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            } finally {
                copyFromDelegate();
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public int translateInterestOps(int ops) {
            try {
                copyToDelegate();
                return invokeMethod(SelChImpl.class, selChImplDelegate, "translateInterestOps", Integer.TYPE, ops, Integer.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            } finally {
                copyFromDelegate();
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public void park(int event, long nanos) throws IOException {
            try {
                copyToDelegate();
                invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Long.TYPE, nanos, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            } finally {
                copyFromDelegate();
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public void park(int event) throws IOException {
            try {
                copyToDelegate();
                invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            } finally {
                copyFromDelegate();
            }
        }

        private static ReflectionFieldCopier[] getReflectionFieldCopiers() {
            if (null == reflectionFieldCopiers) {
                synchronized (SniffySocketChannel.class) {
                    if (null == reflectionFieldCopiers) {
                        List<ReflectionFieldCopier> reflectionFieldCopiersList = new ArrayList<ReflectionFieldCopier>(8); // 4 available on modern Java
                        if (keysCopier.isAvailable()) reflectionFieldCopiersList.add(keysCopier);
                        if (keyCountCopier.isAvailable()) reflectionFieldCopiersList.add(keyCountCopier);
                        if (keyLockCopier.isAvailable()) reflectionFieldCopiersList.add(keyLockCopier);
                        if (regLockCopier.isAvailable()) reflectionFieldCopiersList.add(regLockCopier);
                        if (nonBlockingCopier.isAvailable()) reflectionFieldCopiersList.add(nonBlockingCopier);
                        if (blockingCopier.isAvailable()) reflectionFieldCopiersList.add(blockingCopier);
                        if (closeLockCopier.isAvailable()) reflectionFieldCopiersList.add(closeLockCopier);
                        if (closedCopier.isAvailable()) reflectionFieldCopiersList.add(closedCopier);
                        reflectionFieldCopiers = reflectionFieldCopiersList.toArray(new ReflectionFieldCopier[0]);
                    }
                }
            }
            return reflectionFieldCopiers;
        }

        private void copyToDelegate() {
            for (ReflectionFieldCopier reflectionFieldCopier : getReflectionFieldCopiers()) {
                reflectionFieldCopier.copy(this, delegate);
            }
        }

        private void copyFromDelegate() {
            for (ReflectionFieldCopier reflectionFieldCopier : getReflectionFieldCopiers()) {
                reflectionFieldCopier.copy(delegate, this);
            }
        }

    }

    public static class SniffySinkChannel extends SinkChannel implements SelChImpl {

        private final SinkChannel delegate;
        private final SelChImpl selChImplDelegate;

        private final static ReflectionFieldCopier keysCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "keys");
        private final static ReflectionFieldCopier keyCountCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "keyCount");
        private final static ReflectionFieldCopier keyLockCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "keyLock");
        private final static ReflectionFieldCopier regLockCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "regLock");
        private final static ReflectionFieldCopier nonBlockingCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "nonBlocking");
        private final static ReflectionFieldCopier blockingCopier =
                new ReflectionFieldCopier(AbstractSelectableChannel.class, "blocking");

        private final static ReflectionFieldCopier closeLockCopier =
                new ReflectionFieldCopier(AbstractInterruptibleChannel.class, "closeLock");
        private final static ReflectionFieldCopier closedCopier =
                new ReflectionFieldCopier(AbstractInterruptibleChannel.class, "closed");

        private static volatile ReflectionFieldCopier[] reflectionFieldCopiers;

        public SniffySinkChannel(SelectorProvider provider, SinkChannel delegate) {
            super(provider);
            this.delegate = delegate;
            this.selChImplDelegate = (SelChImpl) delegate;
        }

        @Override
        public void implCloseSelectableChannel() {
            copyToDelegate();
            try {
                invokeMethod(AbstractSelectableChannel.class, delegate, "implCloseSelectableChannel", Void.class);
            } catch (Exception e) {
                ExceptionUtil.processException(e);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public void implConfigureBlocking(boolean block) {
            copyToDelegate();
            try {
                invokeMethod(AbstractSelectableChannel.class, delegate, "implConfigureBlocking", Void.class);
            } catch (Exception e) {
                ExceptionUtil.processException(e);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            copyToDelegate();
            try {
                return delegate.write(srcs, offset, length);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public long write(ByteBuffer[] srcs) throws IOException {
            copyToDelegate();
            try {
                return delegate.write(srcs);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            copyToDelegate();
            try {
                return delegate.write(src);
            } finally {
                copyFromDelegate();
            }
        }

        // Modern SelChImpl

        @Override
        public FileDescriptor getFD() {
            copyToDelegate();
            try {
                return selChImplDelegate.getFD();
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public int getFDVal() {
            copyToDelegate();
            try {
                return selChImplDelegate.getFDVal();
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
            copyToDelegate();
            try {
                return selChImplDelegate.translateAndUpdateReadyOps(ops, ski);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
            copyToDelegate();
            try {
                return selChImplDelegate.translateAndSetReadyOps(ops, ski);
            } finally {
                copyFromDelegate();
            }
        }

        @Override
        public void kill() throws IOException {
            copyToDelegate();
            try {
                selChImplDelegate.kill();
            } finally {
                copyFromDelegate();
            }
        }

        // Note: this method is absent in newer JDKs so we cannot use @Override annotation
        // @Override
        public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
            try {
                copyToDelegate();
                invokeMethod(SelChImpl.class, selChImplDelegate, "translateAndSetInterestOps", Integer.TYPE, ops, SelectionKeyImpl.class, sk, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            } finally {
                copyFromDelegate();
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public int translateInterestOps(int ops) {
            try {
                copyToDelegate();
                return invokeMethod(SelChImpl.class, selChImplDelegate, "translateInterestOps", Integer.TYPE, ops, Integer.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            } finally {
                copyFromDelegate();
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public void park(int event, long nanos) throws IOException {
            try {
                copyToDelegate();
                invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Long.TYPE, nanos, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            } finally {
                copyFromDelegate();
            }
        }

        // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
        //@Override
        public void park(int event) throws IOException {
            try {
                copyToDelegate();
                invokeMethod(SelChImpl.class, selChImplDelegate, "park", Integer.TYPE, event, Void.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.throwException(e);
            } finally {
                copyFromDelegate();
            }
        }

        private static ReflectionFieldCopier[] getReflectionFieldCopiers() {
            if (null == reflectionFieldCopiers) {
                synchronized (SniffySocketChannel.class) {
                    if (null == reflectionFieldCopiers) {
                        List<ReflectionFieldCopier> reflectionFieldCopiersList = new ArrayList<ReflectionFieldCopier>(8); // 4 available on modern Java
                        if (keysCopier.isAvailable()) reflectionFieldCopiersList.add(keysCopier);
                        if (keyCountCopier.isAvailable()) reflectionFieldCopiersList.add(keyCountCopier);
                        if (keyLockCopier.isAvailable()) reflectionFieldCopiersList.add(keyLockCopier);
                        if (regLockCopier.isAvailable()) reflectionFieldCopiersList.add(regLockCopier);
                        if (nonBlockingCopier.isAvailable()) reflectionFieldCopiersList.add(nonBlockingCopier);
                        if (blockingCopier.isAvailable()) reflectionFieldCopiersList.add(blockingCopier);
                        if (closeLockCopier.isAvailable()) reflectionFieldCopiersList.add(closeLockCopier);
                        if (closedCopier.isAvailable()) reflectionFieldCopiersList.add(closedCopier);
                        reflectionFieldCopiers = reflectionFieldCopiersList.toArray(new ReflectionFieldCopier[0]);
                    }
                }
            }
            return reflectionFieldCopiers;
        }

        private void copyToDelegate() {
            for (ReflectionFieldCopier reflectionFieldCopier : getReflectionFieldCopiers()) {
                reflectionFieldCopier.copy(this, delegate);
            }
        }

        private void copyFromDelegate() {
            for (ReflectionFieldCopier reflectionFieldCopier : getReflectionFieldCopiers()) {
                reflectionFieldCopier.copy(delegate, this);
            }
        }

    }

}
