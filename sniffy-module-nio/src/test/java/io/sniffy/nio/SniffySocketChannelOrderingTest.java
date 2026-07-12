package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.NetworkPacket;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.nio.NioTestSupport.daemonThread;
import static io.sniffy.nio.NioTestSupport.joinOrDumpAndFail;
import static org.junit.Assert.*;

public class SniffySocketChannelOrderingTest {

    @BeforeClass
    public static void initializeNioAccess() {
        SniffySelectorProviderModule.initialize();
    }

    @Test
    public void concurrentWritesPreserveCapturedWireOrder() throws Exception {
        assertConcurrentWriteOrder(false, false);
    }

    @Test
    public void concurrentMixedChannelAndStreamWritesPreserveWireOrder() throws Exception {
        assertConcurrentWriteOrder(true, false);
    }

    @Test
    public void concurrentFragmentedConnectIsParsedInPhysicalOrder() throws Exception {
        assertConcurrentWriteOrder(true, true);
    }

    private void assertConcurrentWriteOrder(final boolean secondUsesStream, boolean connect) throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        byte[] firstBytes = connect ? "CON".getBytes("US-ASCII") : new byte[]{'A'};
        byte[] secondBytes = connect
                ? "NECT ordered.example:443 HTTP/1.1\r\n\r\n".getBytes("US-ASCII")
                : new byte[]{'B'};
        byte[] expected = join(firstBytes, secondBytes);
        try (NioTestResourceScope scope = new NioTestResourceScope();
             Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {
            ServerSocketChannel server = scope.track(provider.openServerSocketChannel());
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            SocketChannel raw = provider.openSocketChannel();
            raw.connect(server.getLocalAddress());
            SocketChannel accepted = scope.track(server.accept());
            final PausingHook hook = new PausingHook(true, false);
            final SniffySocketChannel channel = scope.track(new SniffySocketChannel(provider, raw, hook));
            final AtomicReference<Throwable> firstFailure = new AtomicReference<Throwable>();
            final AtomicReference<Throwable> secondFailure = new AtomicReference<Throwable>();
            final CountDownLatch secondStarted = new CountDownLatch(1);
            Thread first = scope.track(daemonThread("ordered-write-first", new Runnable() {
                @Override public void run() {
                    try { channel.write(ByteBuffer.wrap(firstBytes)); }
                    catch (Throwable e) { firstFailure.set(e); }
                }
            }));
            Thread second = scope.track(daemonThread("ordered-write-second", new Runnable() {
                @Override public void run() {
                    secondStarted.countDown();
                    try {
                        if (secondUsesStream) channel.socket().getOutputStream().write(secondBytes);
                        else channel.write(ByteBuffer.wrap(secondBytes));
                    } catch (Throwable e) { secondFailure.set(e); }
                }
            }));

            first.start();
            assertTrue(hook.firstPhysical.await(5, TimeUnit.SECONDS));
            second.start();
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            assertTrue(hook.secondOperationEntered.await(5, TimeUnit.SECONDS));
            assertEquals("the second operation must not pass the physical write while the first event is pending",
                    1, hook.physicalWriteCount.get());
            hook.releaseFirst.countDown();
            joinOrDumpAndFail(first);
            joinOrDumpAndFail(second);
            assertNull(firstFailure.get());
            assertNull(secondFailure.get());

            channel.shutdownOutput();
            assertArrayEquals(expected, readExactly(accepted, expected.length));
            assertArrayEquals(expected, traffic(spy, true));
            if (connect) {
                assertEquals(new InetSocketAddress("ordered.example", 443), channel.getProxiedInetSocketAddress());
            }
        }
    }

    @Test
    public void concurrentReadsPreserveCapturedReadOrder() throws Exception {
        SniffySelectorProvider.uninstall();
        SelectorProvider provider = SelectorProvider.provider();
        try (NioTestResourceScope scope = new NioTestResourceScope();
             Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {
            ServerSocketChannel server = scope.track(provider.openServerSocketChannel());
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            SocketChannel raw = provider.openSocketChannel();
            raw.connect(server.getLocalAddress());
            SocketChannel accepted = scope.track(server.accept());
            final PausingHook hook = new PausingHook(false, true);
            final SniffySocketChannel channel = scope.track(new SniffySocketChannel(provider, raw, hook));
            accepted.write(ByteBuffer.wrap(new byte[]{'A', 'B'}));
            final ByteBuffer firstByte = ByteBuffer.allocate(1);
            final AtomicInteger secondByte = new AtomicInteger(-1);
            final AtomicReference<Throwable> firstFailure = new AtomicReference<Throwable>();
            final AtomicReference<Throwable> secondFailure = new AtomicReference<Throwable>();
            final InputStream stream = channel.socket().getInputStream();
            Thread first = scope.track(daemonThread("ordered-read-first", new Runnable() {
                @Override public void run() {
                    try { channel.read(firstByte); } catch (Throwable e) { firstFailure.set(e); }
                }
            }));
            Thread second = scope.track(daemonThread("ordered-read-second", new Runnable() {
                @Override public void run() {
                    try { secondByte.set(stream.read()); } catch (Throwable e) { secondFailure.set(e); }
                }
            }));

            first.start();
            assertTrue(hook.firstPhysical.await(5, TimeUnit.SECONDS));
            second.start();
            assertTrue(hook.secondOperationEntered.await(5, TimeUnit.SECONDS));
            assertEquals(1, hook.physicalReadCount.get());
            hook.releaseFirst.countDown();
            joinOrDumpAndFail(first);
            joinOrDumpAndFail(second);
            assertNull(firstFailure.get());
            assertNull(secondFailure.get());
            assertEquals('A', firstByte.array()[0]);
            assertEquals('B', secondByte.get());
            assertArrayEquals(new byte[]{'A', 'B'}, traffic(spy, false));
        }
    }

    private static byte[] readExactly(SocketChannel channel, int length) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer);
            if (read < 0) break;
        }
        return buffer.array();
    }

    private static byte[] traffic(Spy<?> spy, boolean sent) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (List<NetworkPacket> packets : spy.getNetworkTraffic().values()) {
            for (NetworkPacket packet : packets) {
                if (packet.isSent() == sent) bytes.write(packet.getBytes());
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] join(byte[] first, byte[] second) {
        byte[] joined = new byte[first.length + second.length];
        System.arraycopy(first, 0, joined, 0, first.length);
        System.arraycopy(second, 0, joined, first.length, second.length);
        return joined;
    }

    private static final class PausingHook implements SniffySocketChannel.IoOperationHook {
        private final boolean pauseWrite;
        private final boolean pauseRead;
        private final AtomicInteger physicalWriteCount = new AtomicInteger();
        private final AtomicInteger physicalReadCount = new AtomicInteger();
        private final CountDownLatch firstPhysical = new CountDownLatch(1);
        private final CountDownLatch releaseFirst = new CountDownLatch(1);
        private final CountDownLatch secondOperationEntered = new CountDownLatch(1);

        private PausingHook(boolean pauseWrite, boolean pauseRead) {
            this.pauseWrite = pauseWrite;
            this.pauseRead = pauseRead;
        }

        @Override public void beforeWrite() {
            signalSecondOperation();
        }

        @Override public void beforeRead() {
            signalSecondOperation();
        }

        @Override public void afterPhysicalWrite() throws java.io.IOException {
            if (pauseWrite && physicalWriteCount.incrementAndGet() == 1) pause();
        }

        @Override public void afterPhysicalRead() throws java.io.IOException {
            if (pauseRead && physicalReadCount.incrementAndGet() == 1) pause();
        }

        @Override public void beforeFinalTrafficPublication() {
        }

        private void signalSecondOperation() {
            if (Thread.currentThread().getName().endsWith("second")) {
                secondOperationEntered.countDown();
            }
        }

        private void pause() throws java.io.IOException {
            firstPhysical.countDown();
            try {
                if (!releaseFirst.await(5, TimeUnit.SECONDS)) throw new java.io.IOException("test hook timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.IOException(e);
            }
        }
    }
}
