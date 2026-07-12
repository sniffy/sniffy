package io.sniffy.nio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
}
