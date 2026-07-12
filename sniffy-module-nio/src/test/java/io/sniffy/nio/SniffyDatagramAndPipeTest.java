package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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

public class SniffyDatagramAndPipeTest {

    @Before
    public void installProvider() {
        SniffySelectorProviderModule.initialize();
    }

    @After
    public void uninstallProvider() {
        SniffySelectorProvider.uninstall();
    }

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
        final Selector selector = Selector.open();
        final CountDownLatch enteringSelect = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger(-1);
        Thread selectingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    enteringSelect.countDown();
                    result.set(selector.select());
                } catch (Exception e) {
                    result.set(-2);
                }
            }
        }, "sniffy-selector-wakeup-test");
        selectingThread.start();
        assertTrue(enteringSelect.await(5, TimeUnit.SECONDS));
        selector.wakeup();
        selectingThread.join(5000);
        try {
            assertFalse(selectingThread.isAlive());
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
            selector.close();
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
        Pipe pipe = Pipe.open();
        final Pipe.SourceChannel source = pipe.source();
        Pipe.SinkChannel sink = pipe.sink();
        final CountDownLatch reading = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    reading.countDown();
                    source.read(ByteBuffer.allocate(1));
                } catch (Throwable e) {
                    failure.set(e);
                }
            }
        }, "sniffy-pipe-blocked-read");
        try {
            reader.start();
            assertTrue(reading.await(5, TimeUnit.SECONDS));
            awaitNativePipeRead(reader);

            try {
                source.close();
            } catch (IOException e) {
                // Some Java 8/macOS builds surface NativeThread.signal(ENOENT) after the
                // channel is already closed. The close contract permits IOException; the
                // invariant under test is that the blocked reader is released.
            }

            reader.join(5000);
            assertFalse(reader.isAlive());
            assertFalse(source.isOpen());
            assertFalse(((SniffyPipe.SniffySourceChannel) source).getDelegate().isOpen());
            Throwable throwable = failure.get();
            assertTrue(throwable == null ||
                    throwable instanceof AsynchronousCloseException ||
                    throwable instanceof ClosedChannelException);
        } finally {
            sink.close();
            source.close();
        }
    }

    private static void awaitNativePipeRead(Thread reader) {
        for (int attempt = 0; attempt < 100000; attempt++) {
            for (StackTraceElement element : reader.getStackTrace()) {
                if ("read0".equals(element.getMethodName()) || "read".equals(element.getMethodName())
                        && element.getClassName().contains("FileDispatcher")) {
                    return;
                }
            }
            Thread.yield();
        }
        fail("reader did not enter the native pipe read; state=" + reader.getState());
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
}
