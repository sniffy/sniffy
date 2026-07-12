package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
}
