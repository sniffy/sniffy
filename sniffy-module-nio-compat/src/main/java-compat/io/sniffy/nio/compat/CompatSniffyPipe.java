package io.sniffy.nio.compat;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionUtil;
import sun.nio.ch.PipeSinkChannelDelegate;
import sun.nio.ch.PipeSourceChannelDelegate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

import static io.sniffy.util.ReflectionUtil.invokeMethod;
import static io.sniffy.util.ReflectionUtil.setField;

/**
 * @since 3.1.7
 */
public class CompatSniffyPipe extends Pipe {

    private final SelectorProvider selectorProvider;
    private final Pipe delegate;

    public CompatSniffyPipe(SelectorProvider selectorProvider, Pipe delegate) {
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
    public static class SniffySourceChannel extends PipeSourceChannelDelegate {

        private final SourceChannel delegate;

        public SniffySourceChannel(SelectorProvider provider, SourceChannel delegate) {
            super(provider, delegate);
            this.delegate = delegate;
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


    }

    @SuppressWarnings("RedundantThrows")
    public static class SniffySinkChannel extends PipeSinkChannelDelegate {

        private final SinkChannel delegate;

        public SniffySinkChannel(SelectorProvider provider, SinkChannel delegate) {
            super(provider, delegate);
            this.delegate = delegate;
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

    }

}
