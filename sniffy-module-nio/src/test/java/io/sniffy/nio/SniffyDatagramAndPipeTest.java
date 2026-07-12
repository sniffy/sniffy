package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import org.junit.Test;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static io.sniffy.nio.NioTestSupport.*;

public class SniffyDatagramAndPipeTest {

    @Test
    public void datagramChannelsAreExplicitlyPassThrough() throws Exception {
        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             DatagramChannel receiver = DatagramChannel.open();
             DatagramChannel sender = DatagramChannel.open();
             Selector selector = Selector.open()) {
            assertFalse(receiver.getClass().getName().startsWith("io.sniffy.nio.Sniffy"));
            receiver.bind(new InetSocketAddress("localhost", 0));
            receiver.configureBlocking(false);
            SelectionKey key = receiver.register(selector, SelectionKey.OP_READ);
            byte[] payload = new byte[]{1, 3, 3, 7};
            assertEquals(payload.length,
                    sender.send(ByteBuffer.wrap(payload), receiver.getLocalAddress()));
            assertEquals(1, selector.select());
            assertSame(receiver, key.channel());
            assertSame(selector, key.selector());
            ByteBuffer received = ByteBuffer.allocate(payload.length);
            receiver.receive(received);

            assertArrayEquals(payload, received.array());
            assertTrue("UDP pass-through must not imply Sniffy traffic capture", spy.getNetworkTraffic().isEmpty());
        }
    }

    @Test
    public void applicationPipeHasStableWrappersAndSelectorLifecycle() throws Exception {
        Pipe pipe = Pipe.open();
        try (Pipe.SourceChannel source = pipe.source(); Pipe.SinkChannel sink = pipe.sink(); Selector selector = Selector.open()) {
            assertTrue(pipe instanceof SniffyPipe);
            assertSame(source, pipe.source());
            assertSame(sink, pipe.sink());
            source.configureBlocking(false);
            SelectionKey key = source.register(selector, SelectionKey.OP_READ);

            sink.write(ByteBuffer.wrap(new byte[]{8}));
            assertEquals(1, selector.select());
            assertSame(source, key.channel());
            assertSame(selector, key.selector());
            ByteBuffer received = ByteBuffer.allocate(1);
            assertEquals(1, source.read(received));
            assertEquals(8, received.array()[0]);
        }
    }

    @Test
    public void selectorWakeupDoesNotLeakInternalConstructionScope() throws Exception {
        SelectorProvider provider = NioFunctionalTestEnvironment.originalProvider();
        final SelectionStartedSelector selector = new SelectionStartedSelector(provider, provider.openSelector());
        final CountDownLatch enteringSelect = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger(-1);
        Thread selectingThread = daemonThread("sniffy-selector-wakeup-test", new Runnable() {
            @Override
            public void run() {
                try {
                    enteringSelect.countDown();
                    result.set(selector.select());
                } catch (Exception e) {
                    result.set(-2);
                }
            }
        });
        try {
            selectingThread.start();
            assertTrue(enteringSelect.await(5, TimeUnit.SECONDS));
            assertTrue(selector.selectionEntered.await(5, TimeUnit.SECONDS));
            selector.wakeup();
            selector.releaseSelection.countDown();
            joinOrDumpAndFail(selectingThread);
            assertEquals(0, result.get());
            assertFalse(SniffySelectorProvider.isDelegateSelectorConstruction());
            Pipe applicationPipe = Pipe.open();
            try {
                assertTrue(applicationPipe instanceof SniffyPipe);
            } finally {
                applicationPipe.source().close();
                applicationPipe.sink().close();
            }
        } finally {
            selector.releaseSelection.countDown();
            selector.wakeup();
            try { selector.close(); } finally {
                joinOrDumpAndFail(selectingThread);
            }
        }
    }

    private static final class SelectionStartedSelector extends SniffySelector {
        private final CountDownLatch selectionEntered = new CountDownLatch(1);
        private final CountDownLatch releaseSelection = new CountDownLatch(1);

        private SelectionStartedSelector(SelectorProvider provider, AbstractSelector delegate) {
            super(provider, delegate);
        }

        @Override void selectionPoint(SelectionPoint point) {
            if (point == SelectionPoint.BEFORE_DELEGATE_SELECT) {
                selectionEntered.countDown();
                try {
                    if (!releaseSelection.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting to release delegate selection");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            super.selectionPoint(point);
        }
    }

    @Test
    public void wrapperCloseClosesDelegateAndInvalidatesKey() throws Exception {
        Pipe pipe = Pipe.open();
        SniffyPipe.SniffySourceChannel source = (SniffyPipe.SniffySourceChannel) pipe.source();
        Pipe.SinkChannel sink = pipe.sink();
        try (Selector selector = Selector.open()) {
            source.configureBlocking(false);
            SelectionKey key = source.register(selector, SelectionKey.OP_READ);

            source.close();

            assertFalse(source.isOpen());
            assertFalse(source.getDelegate().isOpen());
            assertFalse(key.isValid());
        } finally {
            sink.close();
        }
    }

    @Test
    public void repeatedWrapperCloseIsIdempotent() throws Exception {
        Pipe pipe = Pipe.open();
        SniffyPipe.SniffySourceChannel source = (SniffyPipe.SniffySourceChannel) pipe.source();
        Pipe.SinkChannel sink = pipe.sink();
        try {
            source.close();
            source.close();

            assertFalse(source.isOpen());
            assertFalse(source.getDelegate().isOpen());
        } finally {
            sink.close();
        }
    }

    @Test
    public void wrapperCloseUnblocksBlockedPipeRead() throws Exception {
        SelectorProvider provider = NioFunctionalTestEnvironment.originalProvider();
        final BlockingSourceChannel delegate = new BlockingSourceChannel(provider);
        final Pipe.SourceChannel source = new SniffyPipe.SniffySourceChannel(provider, delegate);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread reader = daemonThread("sniffy-pipe-blocked-read", new Runnable() {
            @Override
            public void run() {
                try {
                    source.read(ByteBuffer.allocate(1));
                } catch (Throwable e) {
                    failure.set(e);
                }
            }
        });
        try {
            reader.start();
            assertTrue(delegate.readEntered.await(5, TimeUnit.SECONDS));
            source.close();

            joinOrDumpAndFail(reader);
            assertFalse(source.isOpen());
            assertFalse(delegate.isOpen());
            Throwable throwable = failure.get();
            assertTrue(throwable instanceof AsynchronousCloseException);
        } finally {
            try { source.close(); } finally {
                joinOrDumpAndFail(reader);
            }
        }
    }

    @Test
    public void cancelThenConfigureBlockingTrueDoesNotRequireIntermediateSelect() throws Exception {
        Pipe pipe = Pipe.open();
        SniffyPipe.SniffySourceChannel source = (SniffyPipe.SniffySourceChannel) pipe.source();
        Pipe.SinkChannel sink = pipe.sink();
        try (Selector selector = Selector.open()) {
            source.configureBlocking(false);
            SelectionKey key = source.register(selector, SelectionKey.OP_READ);
            SelectionKey delegateKey = ((SniffySelectionKey) key).getDelegate();

            key.cancel();
            assertFalse(key.isValid());
            assertFalse(delegateKey.isValid());

            source.configureBlocking(true);

            assertTrue(source.isBlocking());
            assertTrue(source.getDelegate().isBlocking());
            assertFalse(delegateKey.isValid());
        } finally {
            source.close();
            sink.close();
        }
    }

    @Test
    public void configureBlockingStateIsKeptInSync() throws Exception {
        Pipe pipe = Pipe.open();
        SniffyPipe.SniffySourceChannel source = (SniffyPipe.SniffySourceChannel) pipe.source();
        Pipe.SinkChannel sink = pipe.sink();
        try {

            assertTrue(source.isBlocking());
            assertTrue(source.getDelegate().isBlocking());

            source.configureBlocking(false);
            assertFalse(source.isBlocking());
            assertFalse(source.getDelegate().isBlocking());

            source.configureBlocking(true);
            assertTrue(source.isBlocking());
            assertTrue(source.getDelegate().isBlocking());
        } finally {
            source.close();
            sink.close();
        }
    }

    @Test
    public void configureBlockingTrueWithValidKeyFails() throws Exception {
        Pipe pipe = Pipe.open();
        SniffyPipe.SniffySourceChannel source = (SniffyPipe.SniffySourceChannel) pipe.source();
        Pipe.SinkChannel sink = pipe.sink();
        try (Selector selector = Selector.open()) {
            source.configureBlocking(false);
            SelectionKey key = source.register(selector, SelectionKey.OP_READ);

            try {
                source.configureBlocking(true);
                fail("Expected IllegalBlockingModeException");
            } catch (IllegalBlockingModeException expected) {
                assertTrue(key.isValid());
                assertTrue(((SniffySelectionKey) key).getDelegate().isValid());
            }
        } finally {
            source.close();
            sink.close();
        }
    }

    private static final class BlockingSourceChannel extends Pipe.SourceChannel implements SelChImpl {
        private final CountDownLatch readEntered = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);

        private BlockingSourceChannel(SelectorProvider provider) {
            super(provider);
        }

        @Override public int read(ByteBuffer dst) throws IOException {
            readEntered.countDown();
            try {
                closed.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            throw new AsynchronousCloseException();
        }

        @Override public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            return read(dsts[offset]);
        }

        @Override public long read(ByteBuffer[] dsts) throws IOException {
            return read(dsts, 0, dsts.length);
        }

        @Override protected void implCloseSelectableChannel() {
            closed.countDown();
        }

        @Override protected void implConfigureBlocking(boolean block) {
        }

        @Override public FileDescriptor getFD() { return null; }
        @Override public int getFDVal() { return -1; }
        @Override public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) { return false; }
        @Override public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) { return false; }
        @Override public void kill() {
        }
        public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        }
        public int translateInterestOps(int ops) { return ops; }
        public void park(int event, long nanos) {
        }
        public void park(int event) {
        }
    }
}
