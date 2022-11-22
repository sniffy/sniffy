package io.sniffy.nio.compat;

import io.sniffy.nio.NioDelegateHelper;
import io.sniffy.util.ExceptionUtil;
import sun.nio.ch.PipeSinkChannelDelegate;
import sun.nio.ch.PipeSourceChannelDelegate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.spi.SelectorProvider;

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

    }

}
