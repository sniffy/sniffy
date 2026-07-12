package io.sniffy.nio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static io.sniffy.nio.NioTestSupport.*;

public class SniffyServerSocketChannelTest {

    @Before
    public void installProvider() {
        SniffySelectorProviderModule.initialize();
    }

    @After
    public void uninstallProvider() {
        SniffySelectorProvider.uninstall();
    }

    @Test
    public void acceptReturnsFullyMonitoredChannel() throws Exception {
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress("localhost", 0));
            try (SocketChannel client = SocketChannel.open(server.getLocalAddress());
                 SocketChannel accepted = server.accept()) {
                assertTrue(server instanceof SniffyServerSocketChannel);
                assertTrue(accepted instanceof SniffySocketChannel);
                assertSame(server.provider(), accepted.provider());
                assertTrue(accepted.isBlocking());
                assertTrue(accepted.getLocalAddress() instanceof InetSocketAddress);
                assertTrue(accepted.getRemoteAddress() instanceof InetSocketAddress);
                assertTrue(accepted.supportedOptions().contains(StandardSocketOptions.SO_KEEPALIVE));
                assertSame(accepted, accepted.socket().getChannel());

                byte[] payload = new byte[]{1, 2, 3, 5, 8};
                client.write(ByteBuffer.wrap(payload));
                ByteBuffer received = ByteBuffer.allocate(payload.length);
                while (received.hasRemaining()) accepted.read(received);
                assertArrayEquals(payload, received.array());
            }
        }
    }

    @Test
    public void nonBlockingAcceptAndSocketViewFollowChannelContract() throws Exception {
        SniffyServerSocketChannel server = (SniffyServerSocketChannel) ServerSocketChannel.open();
        ServerSocket socket = server.socket();
        try {
            assertSame(socket, server.socket());
            assertSame(server, socket.getChannel());
            socket.bind(new InetSocketAddress("localhost", 0));
            assertTrue(server.getLocalAddress() != null);

            server.configureBlocking(false);
            assertNull(server.accept());
            try {
                socket.accept();
                throw new AssertionError("Expected IllegalBlockingModeException");
            } catch (IllegalBlockingModeException expected) {
                // expected
            }

            socket.close();
            socket.close();
            assertFalse(server.isOpen());
            assertFalse(server.getDelegate().isOpen());
            assertTrue(socket.isClosed());
        } finally {
            server.close();
        }
    }

    @Test
    public void serverSocketAdapterOverridesEveryPublicOverridableMethod() throws Exception {
        for (Method method : ServerSocket.class.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
                Method wrapperMethod = SniffyServerSocket.class.getDeclaredMethod(
                        method.getName(), method.getParameterTypes());
                assertSame(SniffyServerSocket.class, wrapperMethod.getDeclaringClass());
            }
        }
    }

    @Test
    public void socketViewAcceptHonorsSoTimeoutAndTimeoutChanges() throws Exception {
        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            ServerSocket socket = channel.socket();
            socket.bind(new InetSocketAddress("localhost", 0));
            socket.setSoTimeout(40);
            assertEquals(40, socket.getSoTimeout());
            assertAcceptTimesOut(socket);
            socket.setSoTimeout(80);
            assertEquals(80, socket.getSoTimeout());
            assertAcceptTimesOut(socket);
        }
    }

    @Test
    public void socketViewAcceptReturnsStableMonitoredSocketBeforeTimeout() throws Exception {
        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            ServerSocket socket = channel.socket();
            socket.bind(new InetSocketAddress("localhost", 0));
            socket.setSoTimeout(2000);
            try (SocketChannel client = SocketChannel.open(socket.getLocalSocketAddress());
                 Socket accepted = socket.accept()) {
                assertTrue(accepted.getChannel() instanceof SniffySocketChannel);
                assertSame(accepted, accepted.getChannel().socket());
                assertSame(accepted.getChannel(), accepted.getChannel().socket().getChannel());
            }
        }
    }

    @Test
    public void zeroTimeoutBlocksUntilCloseAndCloseIsIdempotent() throws Exception {
        final SniffyServerSocketChannel channel = (SniffyServerSocketChannel) ServerSocketChannel.open();
        final ServerSocketChannel delegate = channel.getDelegate();
        final ServerSocket socket = channel.socket();
        socket.bind(new InetSocketAddress("localhost", 0));
        socket.setSoTimeout(0);
        assertEquals(0, socket.getSoTimeout());
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicReference<Throwable> outcome = new AtomicReference<Throwable>();
        final AtomicReference<Throwable> closeFailure = new AtomicReference<Throwable>();
        Thread accepting = daemonThread("sniffy-server-socket-accept", new Runnable() {
            @Override public void run() {
                started.countDown();
                try { socket.accept(); } catch (Throwable e) { outcome.set(e); }
            }
        });
        Thread closing = daemonThread("sniffy-server-socket-close", new Runnable() {
            @Override public void run() {
                try { channel.close(); } catch (Throwable e) { closeFailure.set(e); }
            }
        });
        try {
            accepting.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            awaitStackFrame(accepting, "ServerSocketChannel", "accept");
            closing.start();

            joinOrDumpAndFail(closing);
            joinOrDumpAndFail(accepting);
            Throwable closeOutcome = closeFailure.get();
            if (closeOutcome != null) {
                // JDK 8/macOS can report a native signal failure after marking both views closed.
                assertTrue(closeOutcome instanceof java.io.IOException);
                assertFalse(channel.isOpen());
                assertFalse(delegate.isOpen());
            }
            channel.close();
            assertFalse(channel.isOpen());
            assertFalse(delegate.isOpen());
            assertTrue(outcome.get() instanceof java.io.IOException);
            assertTrue(socket.isClosed());
        } finally {
            Thread delegateCleanup = null;
            final AtomicReference<Throwable> delegateCleanupFailure = new AtomicReference<Throwable>();
            if (channel.isOpen() || delegate.isOpen()) {
                delegateCleanup = daemonThread("sniffy-server-socket-delegate-cleanup", new Runnable() {
                    @Override public void run() {
                        try { delegate.close(); } catch (Throwable e) { delegateCleanupFailure.set(e); }
                    }
                });
                delegateCleanup.start();
            }
            joinOrDumpAndFail(delegateCleanup);
            if (delegateCleanupFailure.get() != null) {
                delegateCleanupFailure.get().printStackTrace(System.err);
            }
            joinOrDumpAndFail(closing);
            joinOrDumpAndFail(accepting);
        }
    }

    private static void assertAcceptTimesOut(ServerSocket socket) throws Exception {
        try {
            socket.accept();
            throw new AssertionError("Expected SocketTimeoutException");
        } catch (SocketTimeoutException expected) {
            // expected
        }
    }
}
