package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.NetworkPacket;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.nio.NioTestSupport.daemonThread;
import static io.sniffy.nio.NioTestSupport.joinOrDumpAndFail;
import static org.junit.Assert.*;

public class SniffySocketChannelPolicyLifecycleTest {

    private static final byte[] DENIED_CONNECT = bytes("CONNECT 127.0.0.1:55443 HTTP/1.1\r\n\r\n");

    @BeforeClass
    public static void initializeNioAccess() {
        SniffySelectorProviderModule.initialize();
    }

    @Test
    public void singleStreamWriteCompletingDeniedConnectSucceedsAndNextOperationFails() throws Exception {
        withDeniedProxy(new ProxyOperation() {
            @Override public void run(ConnectedPair pair) throws Exception {
                OutputStream output = pair.channel.socket().getOutputStream();
                output.write(DENIED_CONNECT);
                assertDeniedOnNextWrite(new WriteAttempt() {
                    @Override public void write() throws Exception { output.write('x'); }
                });
            }
        });
    }

    @Test
    public void singleChannelWriteCompletingDeniedConnectSucceedsAndNextOperationFails() throws Exception {
        withDeniedProxy(new ProxyOperation() {
            @Override public void run(ConnectedPair pair) throws Exception {
                assertEquals(DENIED_CONNECT.length, pair.channel.write(ByteBuffer.wrap(DENIED_CONNECT)));
                assertDeniedOnNextWrite(new WriteAttempt() {
                    @Override public void write() throws Exception { pair.channel.write(ByteBuffer.wrap(new byte[]{'x'})); }
                });
            }
        });
    }

    @Test
    public void gatheringWriteCompletingDeniedConnectUsesSameSemantics() throws Exception {
        withDeniedProxy(new ProxyOperation() {
            @Override public void run(ConnectedPair pair) throws Exception {
                ByteBuffer first = ByteBuffer.wrap(DENIED_CONNECT, 0, 5);
                ByteBuffer second = ByteBuffer.wrap(DENIED_CONNECT, 5, DENIED_CONNECT.length - 5);
                assertEquals(DENIED_CONNECT.length, pair.channel.write(new ByteBuffer[]{first, second}));
                assertDeniedOnNextWrite(new WriteAttempt() {
                    @Override public void write() throws Exception { pair.channel.write(ByteBuffer.wrap(new byte[]{'x'})); }
                });
            }
        });
    }

    private void withDeniedProxy(ProxyOperation operation) throws Exception {
        Boolean previousFault = SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled();
        SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(true);
        assertTrue("proxy interception must be enabled for this fixture",
                Boolean.TRUE.equals(SniffyConfiguration.INSTANCE.getInterceptProxyConnections()));
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 55443, -1);
        try (ConnectedPair pair = ConnectedPair.open()) {
            operation.run(pair);
            assertEquals(new InetSocketAddress("127.0.0.1", 55443), pair.channel.getProxiedInetSocketAddress());
            assertEquals(Integer.valueOf(-1), pair.channel.connectionStatus());
        } finally {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 55443, 0);
            SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(previousFault);
        }
    }

    private static void assertDeniedOnNextWrite(WriteAttempt attempt) throws Exception {
        try {
            attempt.write();
            fail("The policy discovered by the preceding write must apply to this operation");
        } catch (ConnectException expected) {
            // expected
        }
    }

    @Test
    public void operationNeverSeesNewAddressWithOldStatus() throws Exception {
        Boolean previousFault = SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled();
        SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(true);
        try (ConnectedPair pair = ConnectedPair.open(false)) {
            final PolicyPausingHook hook = new PolicyPausingHook();
            final SniffySocketChannel channel = new SniffySocketChannel(SelectorProvider.provider(), pair.raw, hook);
            InetSocketAddress target = new InetSocketAddress("127.0.0.1", 55444);
            channel.setConnectionStatus(0);
            final AtomicReference<Throwable> writeFailure = new AtomicReference<Throwable>();
            Thread writer = daemonThread("policy-snapshot-writer", new Runnable() {
                @Override public void run() {
                    try { channel.write(ByteBuffer.wrap(new byte[]{'A'})); }
                    catch (Throwable e) { writeFailure.set(e); }
                }
            });
            try {
                writer.start();
                assertTrue(hook.physicalWriteCompleted.await(5, TimeUnit.SECONDS));
                channel.setProxiedInetSocketAddressAndStatus(target, -23);
                hook.releaseAccounting.countDown();
                joinOrDumpAndFail(writer);
                assertNull("the in-flight operation must retain its starting allowed snapshot", writeFailure.get());

                SniffySocketChannel.EffectiveEndpointPolicy snapshot = channel.effectiveEndpointPolicy();
                assertSame(snapshot, channel.effectiveEndpointPolicy());
                assertEquals(target, snapshot.address);
                assertEquals(Integer.valueOf(-23), snapshot.status);
                assertTrue(snapshot.proxied);
                assertDeniedOnNextWrite(new WriteAttempt() {
                    @Override public void write() throws Exception { channel.write(ByteBuffer.wrap(new byte[]{'B'})); }
                });
            } finally {
                hook.releaseAccounting.countDown();
                try { channel.close(); } finally { joinOrDumpAndFail(writer); }
            }
        } finally {
            SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(previousFault);
        }
    }

    @Test
    public void physicalProxyStatusUpdateDoesNotOverwriteTargetPolicy() throws Exception {
        try (ConnectedPair pair = ConnectedPair.open()) {
            ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(pair.physicalAddress, pair.channel);
            pair.channel.processOutboundBytes(bytes("CONNECT 127.0.0.1:55445 HTTP/1.1\r\n\r\n"), false);
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(
                    pair.physicalAddress.getAddress().getHostAddress(), pair.physicalAddress.getPort(), 91);
            assertEquals(Integer.valueOf(0), pair.channel.connectionStatus());
            assertEquals(55445, pair.channel.effectiveEndpointPolicy().address.getPort());
        } finally {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 55445, 0);
        }
    }

    @Test
    public void targetStatusUpdateChangesActiveTargetPolicy() throws Exception {
        try (ConnectedPair pair = ConnectedPair.open()) {
            pair.channel.processOutboundBytes(bytes("CONNECT 127.0.0.1:55446 HTTP/1.1\r\n\r\n"), false);
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 55446, 37);
            assertEquals(Integer.valueOf(37), pair.channel.connectionStatus());
            assertEquals(55446, pair.channel.effectiveEndpointPolicy().address.getPort());
        } finally {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 55446, 0);
        }
    }

    @Test
    public void wildcardStatusUpdateAppliesToCurrentEffectiveEndpoint() throws Exception {
        try (ConnectedPair pair = ConnectedPair.open()) {
            ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(pair.physicalAddress, pair.channel);
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, 43);

            SniffySocketChannel.EffectiveEndpointPolicy policy = pair.channel.effectiveEndpointPolicy();
            assertEquals(pair.physicalAddress, policy.address);
            assertEquals(Integer.valueOf(43), policy.status);
            assertFalse(policy.proxied);
        } finally {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, 0);
        }
    }

    @Test
    public void wildcardUpdateAfterProxyDetectionUpdatesTargetPolicy() throws Exception {
        try (ConnectedPair pair = ConnectedPair.open()) {
            pair.channel.processOutboundBytes(
                    bytes("CONNECT 127.0.0.1:55449 HTTP/1.1\r\n\r\n"), false);
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, 57);

            SniffySocketChannel.EffectiveEndpointPolicy policy = pair.channel.effectiveEndpointPolicy();
            assertEquals(new InetSocketAddress("127.0.0.1", 55449), policy.address);
            assertEquals(Integer.valueOf(57), policy.status);
            assertTrue(policy.proxied);
        } finally {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, 0);
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 55449, 0);
        }
    }

    @Test
    public void threadLocalRegistryResolutionRemainsCorrect() throws Exception {
        Boolean previousFault = SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled();
        ConnectionsRegistry.INSTANCE.setThreadLocal(true);
        SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(true);
        try (ConnectedPair pair = ConnectedPair.open()) {
            InetSocketAddress target = new InetSocketAddress("127.0.0.1", 55447);
            pair.channel.setProxiedInetSocketAddressAndStatus(target, 0);
            setThreadLocalStatus(target, -1);
            try {
                pair.channel.checkConnectionAllowed(0);
                fail("current thread policy must deny");
            } catch (ConnectException expected) {
                // expected
            }
            final AtomicReference<Throwable> otherFailure = new AtomicReference<Throwable>();
            Thread other = daemonThread("thread-local-nio-policy", new Runnable() {
                @Override public void run() {
                    try {
                        setThreadLocalStatus(target, 0);
                        pair.channel.checkConnectionAllowed(0);
                    } catch (Throwable e) { otherFailure.set(e); }
                }
            });
            other.start();
            joinOrDumpAndFail(other);
            assertNull(otherFailure.get());
            assertEquals(Integer.valueOf(0), pair.channel.connectionStatus());
        } finally {
            ConnectionsRegistry.INSTANCE.setThreadLocal(false);
            SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(previousFault);
        }
    }

    private static void setThreadLocalStatus(InetSocketAddress address, int status) {
        Map<Map.Entry<String, Integer>, Integer> statuses =
                new HashMap<Map.Entry<String, Integer>, Integer>();
        statuses.put(new AbstractMap.SimpleEntry<String, Integer>(address.getHostString(), address.getPort()), status);
        ConnectionsRegistry.INSTANCE.setThreadLocalDiscoveredAddresses(statuses);
    }

    @Test
    public void outputStreamCloseClosesWrapperAndDelegate() throws Exception {
        try (ConnectedPair pair = ConnectedPair.open()) {
            pair.channel.socket().getOutputStream().close();
            assertFalse(pair.channel.isOpen());
            assertFalse(pair.raw.isOpen());
        }
    }

    @Test
    public void inputStreamCloseClosesWrapperAndDelegate() throws Exception {
        try (ConnectedPair pair = ConnectedPair.open()) {
            pair.channel.socket().getInputStream().close();
            assertFalse(pair.channel.isOpen());
            assertFalse(pair.raw.isOpen());
        }
    }

    @Test
    public void streamCloseInvalidatesRegisteredKey() throws Exception {
        SelectorProvider provider = SelectorProvider.provider();
        try (ConnectedPair pair = ConnectedPair.open();
             SniffySelector selector = new SniffySelector(provider, (AbstractSelector) provider.openSelector())) {
            pair.channel.configureBlocking(false);
            SelectionKey key = pair.channel.register(selector, SelectionKey.OP_READ);
            pair.channel.socket().getOutputStream().close();
            assertFalse(key.isValid());
            assertFalse(((SniffySelectionKey) key).getDelegate().isValid());
            selector.selectNow();
            assertNull(pair.channel.keyFor(selector));
            assertEquals(0, selector.activeLinkCount());
        }
    }

    @Test
    public void outputStreamCloseFinalizesPendingPrefixExactlyOnce() throws Exception {
        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             ConnectedPair pair = ConnectedPair.open()) {
            OutputStream output = pair.channel.socket().getOutputStream();
            output.write(bytes("CON"));
            assertEquals(3, pair.channel.pendingInitialOutboundByteCount());
            output.close();
            output.close();
            assertArrayEquals(bytes("CON"), sentTraffic(spy));
            assertEquals(0, pair.channel.pendingInitialOutboundByteCount());
        }
    }

    @Test
    public void streamCloseStillClosesWhenCurrentPolicyIsDenied() throws Exception {
        Boolean previousFault = SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled();
        SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(true);
        try (ConnectedPair pair = ConnectedPair.open()) {
            OutputStream output = pair.channel.socket().getOutputStream();
            pair.channel.setProxiedInetSocketAddressAndStatus(new InetSocketAddress("127.0.0.1", 55448), -1);
            output.close();
            assertFalse(pair.channel.isOpen());
            assertFalse(pair.raw.isOpen());
        } finally {
            SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(previousFault);
        }
    }

    @Test
    public void repeatedStreamCloseIsIdempotent() throws Exception {
        try (ConnectedPair pair = ConnectedPair.open()) {
            InputStream input = pair.channel.socket().getInputStream();
            input.close();
            input.close();
            pair.channel.close();
            assertFalse(pair.raw.isOpen());
        }
    }

    @Test
    public void telemetryFailureCannotPreventPhysicalClose() throws Exception {
        SelectorProvider provider = SelectorProvider.provider();
        try (Spy<?> ignored = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             ConnectedPair pair = ConnectedPair.open(false)) {
            SniffySocketChannel channel = new SniffySocketChannel(provider, pair.raw, new ThrowingTrafficHook());
            channel.processOutboundBytes(bytes("CON"), true);
            try {
                channel.close();
                fail("telemetry failure expected");
            } catch (RuntimeException expected) {
                assertEquals("injected traffic failure", expected.getMessage());
            }
            assertFalse(pair.raw.isOpen());
        }
    }

    @Test
    public void telemetryFailureCannotPreventPhysicalShutdownOutput() throws Exception {
        SelectorProvider provider = SelectorProvider.provider();
        try (Spy<?> ignored = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             ConnectedPair pair = ConnectedPair.open(false)) {
            SniffySocketChannel channel = new SniffySocketChannel(provider, pair.raw, new ThrowingTrafficHook());
            channel.processOutboundBytes(bytes("CON"), true);
            try {
                channel.shutdownOutput();
                fail("telemetry failure expected");
            } catch (RuntimeException expected) {
                assertEquals("injected traffic failure", expected.getMessage());
            }
            assertTrue(pair.raw.socket().isOutputShutdown());
            channel.close();
        }
    }

    @Test
    public void eofIsNotAccountedAsNegativeTrafficAcrossStreamAndChannelReads() throws Exception {
        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build());
             ConnectedPair pair = ConnectedPair.open()) {
            pair.accepted.shutdownOutput();
            InputStream input = pair.channel.socket().getInputStream();
            pair.channel.setPotentiallyBufferedInputBytes(11);
            assertEquals(-1, input.read());
            assertEquals(-1, input.read(new byte[4]));
            assertEquals(-1, input.read(new byte[6], 1, 3));
            assertEquals(-1, pair.channel.read(ByteBuffer.allocate(2)));
            assertEquals(11, pair.channel.getPotentiallyBufferedInputBytes());
            assertArrayEquals(new byte[0], receivedTraffic(spy));
        }
    }

    private static byte[] sentTraffic(Spy<?> spy) throws Exception { return traffic(spy, true); }
    private static byte[] receivedTraffic(Spy<?> spy) throws Exception { return traffic(spy, false); }

    private static byte[] traffic(Spy<?> spy, boolean sent) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (List<NetworkPacket> packets : spy.getNetworkTraffic().values()) {
            for (NetworkPacket packet : packets) {
                if (packet.isSent() == sent) bytes.write(packet.getBytes());
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] bytes(String value) {
        try { return value.getBytes("US-ASCII"); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    private interface ProxyOperation { void run(ConnectedPair pair) throws Exception; }
    private interface WriteAttempt { void write() throws Exception; }

    private static final class ConnectedPair implements AutoCloseable {
        final ServerSocketChannel server;
        final SocketChannel raw;
        final SocketChannel accepted;
        final SniffySocketChannel channel;
        final InetSocketAddress physicalAddress;

        static ConnectedPair open() throws Exception { return open(true); }

        static ConnectedPair open(boolean wrap) throws Exception {
            SniffySelectorProvider.uninstall();
            SelectorProvider provider = SelectorProvider.provider();
            ServerSocketChannel server = provider.openServerSocketChannel();
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            SocketChannel raw = provider.openSocketChannel();
            raw.connect(server.getLocalAddress());
            SocketChannel accepted = server.accept();
            return new ConnectedPair(server, raw, accepted,
                    wrap ? new SniffySocketChannel(provider, raw) : null);
        }

        private ConnectedPair(ServerSocketChannel server, SocketChannel raw, SocketChannel accepted,
                              SniffySocketChannel channel) throws Exception {
            this.server = server;
            this.raw = raw;
            this.accepted = accepted;
            this.channel = channel;
            this.physicalAddress = (InetSocketAddress) raw.getRemoteAddress();
        }

        @Override public void close() throws Exception {
            Throwable failure = null;
            if (channel != null) try { channel.close(); } catch (Throwable e) { failure = e; }
            try { raw.close(); } catch (Throwable e) { failure = suppress(failure, e); }
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

    private static final class ThrowingTrafficHook implements SniffySocketChannel.IoOperationHook {
        @Override public void beforeWrite() {
        }
        @Override public void beforeRead() {
        }
        @Override public void afterPhysicalWrite() {
        }
        @Override public void afterPhysicalRead() {
        }
        @Override public void beforeFinalTrafficPublication() {
            throw new RuntimeException("injected traffic failure");
        }
    }

    private static final class PolicyPausingHook implements SniffySocketChannel.IoOperationHook {
        private final CountDownLatch physicalWriteCompleted = new CountDownLatch(1);
        private final CountDownLatch releaseAccounting = new CountDownLatch(1);

        @Override public void beforeWrite() {
        }
        @Override public void beforeRead() {
        }
        @Override public void afterPhysicalRead() {
        }
        @Override public void beforeFinalTrafficPublication() {
        }
        @Override public void afterPhysicalWrite() throws java.io.IOException {
            physicalWriteCompleted.countDown();
            try {
                if (!releaseAccounting.await(5, TimeUnit.SECONDS)) {
                    throw new java.io.IOException("policy test hook timed out");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.IOException(e);
            }
        }
    }
}
