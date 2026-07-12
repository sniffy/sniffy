package io.sniffy.nio;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;

public class UnixDomainPassThroughTest {

    @Test
    public void unixSocketAndServerChannelsRemainPassThroughAndFunctional() throws Exception {
        ProtocolFamily unix = family("UNIX");
        SocketAddress address = unixAddress();
        Path path = (Path) address.getClass().getMethod("getPath").invoke(address);
        Files.deleteIfExists(path);

        try (ServerSocketChannel server = openServer(SelectorProvider.provider(), unix);
             SocketChannel client = openSocket(SelectorProvider.provider(), unix)) {
            assertFalse(server instanceof SniffyServerSocketChannel);
            assertFalse(client instanceof SniffySocketChannel);
            server.bind(address);
            client.connect(address);
            try (SocketChannel accepted = server.accept()) {
                assertFalse(accepted instanceof SniffySocketChannel);
                client.write(ByteBuffer.wrap(new byte[]{3, 1, 4}));
                ByteBuffer bytes = ByteBuffer.allocate(3);
                while (bytes.hasRemaining()) accepted.read(bytes);
                assertArrayEquals(new byte[]{3, 1, 4}, bytes.array());
                accepted.write(ByteBuffer.wrap(new byte[]{2, 7}));
                ByteBuffer reply = ByteBuffer.allocate(2);
                while (reply.hasRemaining()) client.read(reply);
                assertArrayEquals(new byte[]{2, 7}, reply.array());
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void unixUnsupportedSocketViewBehaviorMatchesOriginalProvider() throws Exception {
        ProtocolFamily unix = family("UNIX");
        Class<?> originalFailure;
        try (SocketChannel raw = openSocket(NioFunctionalTestEnvironment.originalProvider(), unix)) {
            originalFailure = failureFromSocketView(raw);
        }
        try (SocketChannel passThrough = openSocket(SelectorProvider.provider(), unix)) {
            assertEquals(originalFailure, failureFromSocketView(passThrough));
        }
    }

    @Test
    public void ipProtocolFamilyOverloadsRemainMonitored() throws Exception {
        ProtocolFamily inet = family("INET");
        try (SocketChannel socket = openSocket(SelectorProvider.provider(), inet);
             ServerSocketChannel server = openServer(SelectorProvider.provider(), inet)) {
            assertTrue(socket instanceof SniffySocketChannel);
            assertTrue(server instanceof SniffyServerSocketChannel);
        }
    }

    private static Class<?> failureFromSocketView(SocketChannel channel) {
        try {
            channel.socket();
            return null;
        } catch (Throwable failure) {
            return failure.getClass();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ProtocolFamily family(String name) {
        try {
            Class enumClass = Class.forName("java.net.StandardProtocolFamily");
            return (ProtocolFamily) Enum.valueOf(enumClass, name);
        } catch (Throwable unsupported) {
            assumeNoException(unsupported);
            throw new AssertionError(unsupported);
        }
    }

    private static SocketAddress unixAddress() {
        try {
            Path path = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"),
                    "sniffy-" + System.nanoTime() + ".sock");
            Class<?> type = Class.forName("java.net.UnixDomainSocketAddress");
            return (SocketAddress) type.getMethod("of", Path.class).invoke(null, path);
        } catch (Throwable unsupported) {
            assumeNoException(unsupported);
            throw new AssertionError(unsupported);
        }
    }

    private static SocketChannel openSocket(SelectorProvider provider, ProtocolFamily family) throws Exception {
        return (SocketChannel) invoke(provider, "openSocketChannel", family);
    }

    private static ServerSocketChannel openServer(SelectorProvider provider, ProtocolFamily family) throws Exception {
        return (ServerSocketChannel) invoke(provider, "openServerSocketChannel", family);
    }

    private static Object invoke(SelectorProvider provider, String methodName, ProtocolFamily family) throws Exception {
        try {
            Method method = SelectorProvider.class.getMethod(methodName, ProtocolFamily.class);
            return method.invoke(provider, family);
        } catch (NoSuchMethodException unsupported) {
            assumeNoException(unsupported);
            throw unsupported;
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof UnsupportedOperationException) {
                assumeNoException(cause);
            }
            if (cause instanceof Exception) throw (Exception) cause;
            throw (Error) cause;
        }
    }
}
