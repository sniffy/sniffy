package io.sniffy.nio;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.nio.NioTestSupport.daemonThread;
import static io.sniffy.nio.NioTestSupport.joinOrDumpAndFail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SniffySocketChannelAccountingTest {

    @BeforeClass
    public static void initializeNioAccess() {
        SniffySelectorProviderModule.initialize();
    }

    @Test
    public void concurrentReadAndWriteDoNotLoseBufferAccountingUpdates() throws Exception {
        FullDuplexHook hook = new FullDuplexHook(true);
        try (ConnectedChannel fixture = ConnectedChannel.open(hook)) {
            fixture.channel.setPotentiallyBufferedInputBytes(100);
            fixture.channel.setPotentiallyBufferedOutputBytes(100);
            fixture.accepted.write(ByteBuffer.wrap(new byte[]{'R'}));
            OperationThreads operations = new OperationThreads(fixture.channel);
            try {
                operations.start();
                assertTrue(hook.readPhysical.await(5, TimeUnit.SECONDS));
                assertTrue(hook.writePhysical.await(5, TimeUnit.SECONDS));
                hook.releaseAccounting.countDown();
                operations.join();

                assertNull(operations.readFailure.get());
                assertNull(operations.writeFailure.get());
                assertEquals(99, fixture.channel.getPotentiallyBufferedInputBytes());
                assertEquals(99, fixture.channel.getPotentiallyBufferedOutputBytes());
                assertEquals(operations.reader.getId(), fixture.channel.getLastReadThreadId());
                assertEquals(operations.writer.getId(), fixture.channel.getLastWriteThreadId());
            } finally {
                hook.releaseAccounting.countDown();
                operations.join();
            }
        }
    }

    @Test
    public void directionSwitchAccountingUsesOneConsistentState() throws Exception {
        try (ConnectedChannel fixture = ConnectedChannel.open(SniffySocketChannelAccountingTest.noopHook())) {
            long currentThread = Thread.currentThread().getId();
            fixture.channel.setPotentiallyBufferedInputBytes(10);
            fixture.channel.setPotentiallyBufferedOutputBytes(20);
            fixture.channel.setLastReadThreadId(-1L);
            fixture.channel.setLastWriteThreadId(currentThread);
            fixture.accepted.write(ByteBuffer.wrap(new byte[]{'R'}));

            assertEquals(1, fixture.channel.read(ByteBuffer.allocate(1)));

            assertEquals(9, fixture.channel.getPotentiallyBufferedInputBytes());
            assertEquals(0, fixture.channel.getPotentiallyBufferedOutputBytes());
            assertEquals(currentThread, fixture.channel.getLastReadThreadId());
            assertEquals(currentThread, fixture.channel.getLastWriteThreadId());
        }
    }

    @Test
    public void accountingLockDoesNotSerializePhysicalFullDuplexIO() throws Exception {
        FullDuplexHook hook = new FullDuplexHook(false);
        try (ConnectedChannel fixture = ConnectedChannel.open(hook)) {
            fixture.accepted.write(ByteBuffer.wrap(new byte[]{'R'}));
            final OperationThreads operations = new OperationThreads(fixture.channel);
            Object accountingLock = accountingLock(fixture.channel);
            try {
                synchronized (accountingLock) {
                    operations.start();
                    assertTrue("physical read must not require the accounting lock",
                            hook.readPhysical.await(5, TimeUnit.SECONDS));
                    assertTrue("physical write must not require the accounting lock",
                            hook.writePhysical.await(5, TimeUnit.SECONDS));
                    assertTrue(operations.reader.isAlive());
                    assertTrue(operations.writer.isAlive());
                }
                operations.join();
                assertNull(operations.readFailure.get());
                assertNull(operations.writeFailure.get());
            } finally {
                operations.join();
            }
        }
    }

    private static Object accountingLock(SniffySocketChannel channel) throws Exception {
        Field field = SniffySocketChannel.class.getDeclaredField("bufferAccountingLock");
        field.setAccessible(true);
        return field.get(channel);
    }

    private static SniffySocketChannel.IoOperationHook noopHook() {
        return new SniffySocketChannel.IoOperationHook() {
            @Override public void beforeWrite() {
            }
            @Override public void beforeRead() {
            }
            @Override public void afterPhysicalWrite() {
            }
            @Override public void afterPhysicalRead() {
            }
            @Override public void beforeFinalTrafficPublication() {
            }
        };
    }

    private static final class OperationThreads {
        private final AtomicReference<Throwable> readFailure = new AtomicReference<Throwable>();
        private final AtomicReference<Throwable> writeFailure = new AtomicReference<Throwable>();
        private final Thread reader;
        private final Thread writer;

        private OperationThreads(final SniffySocketChannel channel) {
            reader = daemonThread("sniffy-accounting-reader", new Runnable() {
                @Override public void run() {
                    try { channel.read(ByteBuffer.allocate(1)); }
                    catch (Throwable e) { readFailure.set(e); }
                }
            });
            writer = daemonThread("sniffy-accounting-writer", new Runnable() {
                @Override public void run() {
                    try { channel.write(ByteBuffer.wrap(new byte[]{'W'})); }
                    catch (Throwable e) { writeFailure.set(e); }
                }
            });
        }

        private void start() {
            reader.start();
            writer.start();
        }

        private void join() throws Exception {
            try { joinOrDumpAndFail(reader); } finally { joinOrDumpAndFail(writer); }
        }
    }

    private static final class FullDuplexHook implements SniffySocketChannel.IoOperationHook {
        private final boolean pauseAfterPhysical;
        private final CountDownLatch readPhysical = new CountDownLatch(1);
        private final CountDownLatch writePhysical = new CountDownLatch(1);
        private final CountDownLatch releaseAccounting = new CountDownLatch(1);

        private FullDuplexHook(boolean pauseAfterPhysical) {
            this.pauseAfterPhysical = pauseAfterPhysical;
        }

        @Override public void beforeWrite() {
        }
        @Override public void beforeRead() {
        }
        @Override public void afterPhysicalWrite() throws java.io.IOException {
            writePhysical.countDown();
            awaitRelease();
        }
        @Override public void afterPhysicalRead() throws java.io.IOException {
            readPhysical.countDown();
            awaitRelease();
        }
        @Override public void beforeFinalTrafficPublication() {
        }

        private void awaitRelease() throws java.io.IOException {
            if (!pauseAfterPhysical) return;
            try {
                if (!releaseAccounting.await(5, TimeUnit.SECONDS)) {
                    throw new java.io.IOException("accounting test hook timed out");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.IOException(e);
            }
        }
    }

    private static final class ConnectedChannel implements AutoCloseable {
        private final ServerSocketChannel server;
        private final SocketChannel accepted;
        private final SniffySocketChannel channel;

        private static ConnectedChannel open(SniffySocketChannel.IoOperationHook hook) throws Exception {
            SniffySelectorProvider.uninstall();
            SelectorProvider provider = SelectorProvider.provider();
            ServerSocketChannel server = provider.openServerSocketChannel();
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            SocketChannel raw = provider.openSocketChannel();
            raw.connect(server.getLocalAddress());
            SocketChannel accepted = server.accept();
            return new ConnectedChannel(server, accepted, new SniffySocketChannel(provider, raw, hook));
        }

        private ConnectedChannel(ServerSocketChannel server, SocketChannel accepted, SniffySocketChannel channel) {
            this.server = server;
            this.accepted = accepted;
            this.channel = channel;
        }

        @Override public void close() throws Exception {
            Throwable failure = null;
            try { channel.close(); } catch (Throwable e) { failure = e; }
            try { accepted.close(); } catch (Throwable e) { failure = suppress(failure, e); }
            try { server.close(); } catch (Throwable e) { failure = suppress(failure, e); }
            if (failure instanceof Exception) throw (Exception) failure;
            if (failure instanceof Error) throw (Error) failure;
        }

        private static Throwable suppress(Throwable primary, Throwable secondary) {
            if (primary == null) return secondary;
            primary.addSuppressed(secondary);
            return primary;
        }
    }
}
