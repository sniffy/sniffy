package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.socket.SniffySSLNetworkConnection;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class SniffySocketChannelTlsCorrelationTest {

    private static final byte[] CONNECT = bytes("CONNECT tls.example:443 HTTP/1.1\r\n\r\n");
    private static final byte[] PLAINTEXT = bytes("plain-http");
    private static final byte[] CLIENT_HELLO = new byte[]{22, 3, 3, 0, 3, 1, 2, 3};
    private static final byte[] LATER_CLIENT_HELLO = new byte[]{22, 3, 3, 0, 3, 4, 5, 6};

    @Test
    public void directPlaintextConsumesTlsFirstChunkOnCacheMiss() throws Exception {
        TestSslConnection lateMatch = new TestSslConnection();
        try (NioTestStateScope state = new NioTestStateScope()
                     .preserveClientHello(ByteBuffer.wrap(PLAINTEXT));
             Spy<?> ignored = captureTraffic(); ConnectedChannel fixture = ConnectedChannel.open()) {
            fixture.channel.logTraffic(true, Protocol.TCP, PLAINTEXT, 0, PLAINTEXT.length);
            Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(PLAINTEXT), lateMatch);
            fixture.channel.logTraffic(true, Protocol.TCP, PLAINTEXT, 0, PLAINTEXT.length);
            assertNull(lateMatch.connection);
            assertEquals(0, lateMatch.callbackCount.get());
        }
    }

    @Test
    public void laterClientHelloDoesNotCorrelateAfterPlaintext() throws Exception {
        TestSslConnection laterHello = new TestSslConnection();
        try (NioTestStateScope state = new NioTestStateScope()
                     .preserveClientHello(ByteBuffer.wrap(LATER_CLIENT_HELLO));
             Spy<?> ignored = captureTraffic(); ConnectedChannel fixture = ConnectedChannel.open()) {
            fixture.channel.logTraffic(true, Protocol.TCP, PLAINTEXT, 0, PLAINTEXT.length);
            Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(LATER_CLIENT_HELLO), laterHello);
            fixture.channel.logTraffic(true, Protocol.TCP, LATER_CLIENT_HELLO, 0, LATER_CLIENT_HELLO.length);
            assertNull(laterHello.connection);
        }
    }

    @Test
    public void connectHandshakeDoesNotConsumeTlsFirstChunk() throws Exception {
        TestSslConnection firstTunneled = new TestSslConnection();
        try (NioTestStateScope state = new NioTestStateScope()
                     .preserveClientHello(ByteBuffer.wrap(CLIENT_HELLO));
             Spy<?> ignored = captureTraffic(); ConnectedChannel fixture = ConnectedChannel.open()) {
            Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(CLIENT_HELLO), firstTunneled);
            fixture.channel.logTraffic(true, Protocol.TCP, CONNECT, 0, CONNECT.length, true);
            fixture.channel.logTraffic(true, Protocol.TCP, CLIENT_HELLO, 0, CLIENT_HELLO.length, false);
            assertSame(fixture.channel, firstTunneled.connection);
            assertEquals(1, firstTunneled.callbackCount.get());
        }
    }

    @Test
    public void firstTunneledChunkConsumesTlsFirstChunkOnCacheMiss() throws Exception {
        TestSslConnection laterHello = new TestSslConnection();
        try (NioTestStateScope state = new NioTestStateScope()
                     .preserveClientHello(ByteBuffer.wrap(LATER_CLIENT_HELLO));
             Spy<?> ignored = captureTraffic(); ConnectedChannel fixture = ConnectedChannel.open()) {
            fixture.channel.logTraffic(true, Protocol.TCP, CONNECT, 0, CONNECT.length, true);
            fixture.channel.logTraffic(true, Protocol.TCP, PLAINTEXT, 0, PLAINTEXT.length, false);
            Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(LATER_CLIENT_HELLO), laterHello);
            fixture.channel.logTraffic(true, Protocol.TCP,
                    LATER_CLIENT_HELLO, 0, LATER_CLIENT_HELLO.length, false);
            assertNull(laterHello.connection);
        }
    }

    @Test
    public void firstTunneledClientHelloCorrelatesExactlyOnce() throws Exception {
        TestSslConnection firstHello = new TestSslConnection();
        TestSslConnection secondHello = new TestSslConnection();
        try (NioTestStateScope state = new NioTestStateScope()
                     .preserveClientHello(ByteBuffer.wrap(CLIENT_HELLO))
                     .preserveClientHello(ByteBuffer.wrap(LATER_CLIENT_HELLO));
             Spy<?> ignored = captureTraffic(); ConnectedChannel fixture = ConnectedChannel.open()) {
            Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(CLIENT_HELLO), firstHello);
            Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(LATER_CLIENT_HELLO), secondHello);
            fixture.channel.logTraffic(true, Protocol.TCP, CONNECT, 0, CONNECT.length, true);
            fixture.channel.logTraffic(true, Protocol.TCP, CLIENT_HELLO, 0, CLIENT_HELLO.length, false);
            fixture.channel.logTraffic(true, Protocol.TCP,
                    LATER_CLIENT_HELLO, 0, LATER_CLIENT_HELLO.length, false);
            assertSame(fixture.channel, firstHello.connection);
            assertEquals(1, firstHello.callbackCount.get());
            assertNull(secondHello.connection);
            assertEquals(0, secondHello.callbackCount.get());
        }
    }

    private static Spy<?> captureTraffic() {
        return Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
    }

    private static byte[] bytes(String value) {
        try {
            return value.getBytes("US-ASCII");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static final class TestSslConnection implements SniffySSLNetworkConnection {
        private final AtomicInteger callbackCount = new AtomicInteger();
        private SniffyNetworkConnection connection;

        @Override public SniffyNetworkConnection getSniffyNetworkConnection() {
            return connection;
        }

        @Override public void setSniffyNetworkConnection(SniffyNetworkConnection connection) {
            this.connection = connection;
            callbackCount.incrementAndGet();
        }
    }

    private static final class ConnectedChannel implements AutoCloseable {
        private final ServerSocketChannel server;
        private final SocketChannel accepted;
        private final SniffySocketChannel channel;

        private static ConnectedChannel open() throws Exception {
            SelectorProvider provider = NioFunctionalTestEnvironment.originalProvider();
            ServerSocketChannel server = provider.openServerSocketChannel();
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            SocketChannel raw = provider.openSocketChannel();
            raw.connect(server.getLocalAddress());
            SocketChannel accepted = server.accept();
            return new ConnectedChannel(server, accepted, new SniffySocketChannel(provider, raw));
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
