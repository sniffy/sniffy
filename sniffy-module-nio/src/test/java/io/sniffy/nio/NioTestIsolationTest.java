package io.sniffy.nio;

import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.registry.ConnectionsRegistryTestState;
import io.sniffy.socket.SniffyNetworkConnection;
import org.junit.Rule;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class NioTestIsolationTest {

    @Rule public final NioTestStateRule localState = new NioTestStateRule();

    @Test
    public void connectionRegistrationsDoNotLeakAcrossFunctionalTests() throws Exception {
        InetSocketAddress address = aliasedAddress();
        SniffyNetworkConnection completedTestConnection = mock(SniffyNetworkConnection.class);
        try (NioTestStateScope completedTest = new NioTestStateScope().preserveConnectionsRegistry()) {
            ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(address, completedTestConnection);
            assertEquals(2, ConnectionsRegistryTestState.registrationCount(
                    ConnectionsRegistry.INSTANCE, completedTestConnection));
        }

        SniffyNetworkConnection currentTestConnection = mock(SniffyNetworkConnection.class);
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(address, currentTestConnection);
        reset(completedTestConnection, currentTestConnection);
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus(
                address.getAddress().getHostAddress(), address.getPort(), -21);

        verifyNoInteractions(completedTestConnection);
        verify(currentTestConnection, times(1)).setConnectionStatus(any(InetSocketAddress.class), eq(-21));
    }

    @Test
    public void registryScopeRestoresRegisteredConnectionSet() throws Exception {
        InetSocketAddress address = aliasedAddress();
        SniffyNetworkConnection existingConnection = mock(SniffyNetworkConnection.class);
        SniffyNetworkConnection scopedConnection = mock(SniffyNetworkConnection.class);
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(address, existingConnection);

        try (NioTestStateScope scope = new NioTestStateScope().preserveConnectionsRegistry()) {
            ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(address, scopedConnection);
            assertEquals(2, ConnectionsRegistryTestState.registrationCount(
                    ConnectionsRegistry.INSTANCE, scopedConnection));
        }

        assertEquals(2, ConnectionsRegistryTestState.registrationCount(
                ConnectionsRegistry.INSTANCE, existingConnection));
        assertEquals(0, ConnectionsRegistryTestState.registrationCount(
                ConnectionsRegistry.INSTANCE, scopedConnection));
    }

    @Test
    public void wildcardUpdateDoesNotReachConnectionFromCompletedTestScope() throws Exception {
        InetSocketAddress address = aliasedAddress();
        SniffyNetworkConnection completedTestConnection = mock(SniffyNetworkConnection.class);
        try (NioTestStateScope completedTest = new NioTestStateScope().preserveConnectionsRegistry()) {
            ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(address, completedTestConnection);
        }

        reset(completedTestConnection);
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, 31);

        verifyNoInteractions(completedTestConnection);
    }

    @Test
    public void socketChannelCloseUnregistersNetworkConnectionAliases() throws Exception {
        InetSocketAddress address = aliasedAddress();
        SelectorProvider provider = NioFunctionalTestEnvironment.originalProvider();
        SniffySocketChannel channel = new SniffySocketChannel(provider, provider.openSocketChannel());
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(address, channel);
        assertEquals(2, ConnectionsRegistryTestState.registrationCount(ConnectionsRegistry.INSTANCE, channel));

        channel.close();

        assertEquals(0, ConnectionsRegistryTestState.registrationCount(ConnectionsRegistry.INSTANCE, channel));
    }

    @Test
    public void functionalForkStartsWithSniffyProviderInstalled() {
        NioFunctionalTestEnvironment.assertGloballyInstalled();
        assertSame(NioFunctionalTestEnvironment.installedProvider(), SelectorProvider.provider());
    }

    @Test
    public void functionalTestsNeverObserveOriginalProvider() throws Exception {
        try (SocketChannel socket = SocketChannel.open();
             ServerSocketChannel server = ServerSocketChannel.open()) {
            assertTrue(socket instanceof SniffySocketChannel);
            assertTrue(server instanceof SniffyServerSocketChannel);
            NioFunctionalTestEnvironment.assertGloballyInstalled();
        }
    }

    @Test
    public void rawDelegateFixturesDoNotMutateGlobalProvider() throws Exception {
        SelectorProvider original = NioFunctionalTestEnvironment.originalProvider();
        try (SocketChannel rawSocket = original.openSocketChannel();
             ServerSocketChannel rawServer = original.openServerSocketChannel();
             AbstractSelector rawSelector = NioFunctionalTestEnvironment.openRawSelector()) {
            assertFalse(rawSocket instanceof SniffySocketChannel);
            assertFalse(rawServer instanceof SniffyServerSocketChannel);
            assertFalse(rawSelector instanceof SniffySelector);
            assertFalse(SniffySelectorProvider.isDelegateSelectorConstruction());
            assertSame(NioFunctionalTestEnvironment.installedProvider(), SelectorProvider.provider());
        }
    }

    @Test
    public void cleanupUtilityRestoresStateAfterInjectedFailure() throws Exception {
        final AtomicInteger state = new AtomicInteger(7);
        NioTestStateScope scope = new NioTestStateScope()
                .restore(new NioTestStateScope.Cleanup() {
                    @Override public void run() { state.set(7); }
                })
                .restore(new NioTestStateScope.Cleanup() {
                    @Override public void run() { throw new IllegalStateException("second cleanup"); }
                })
                .restore(new NioTestStateScope.Cleanup() {
                    @Override public void run() { throw new IllegalArgumentException("first cleanup"); }
                });
        state.set(99);
        try {
            scope.close();
            fail("cleanup failure expected");
        } catch (IllegalArgumentException expected) {
            assertEquals("first cleanup", expected.getMessage());
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("second cleanup", expected.getSuppressed()[0].getMessage());
        }
        assertEquals(7, state.get());
    }

    private static InetSocketAddress aliasedAddress() throws Exception {
        return new InetSocketAddress(InetAddress.getByAddress(
                "nio-registry-test.invalid", new byte[]{127, 0, 0, 43}), 5556);
    }
}
