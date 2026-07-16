package io.sniffy.test.junit.jupiter;

import io.sniffy.Expectation;
import io.sniffy.NoQueriesAllowed;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.DisableSockets;
import io.sniffy.socket.EchoServerRule;
import io.sniffy.socket.NoSocketsAllowed;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.sql.NoSql;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.util.IOUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.File;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.jupiter.api.Assertions.*;

public class SniffyExtensionLauncherTest {

    @Test
    public void extensionIsNotActiveWithoutRegistration() {
        List<Throwable> failures = execute(UnregisteredNoSqlSample.class);
        org.junit.jupiter.api.Assertions.assertTrue(failures.isEmpty());
    }

    @Test
    public void sniffyFailureIsSuppressedWhenUserAlreadyFailed() {
        List<Throwable> failures = execute(CombinedFailureSample.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, failures.size());
        org.junit.jupiter.api.Assertions.assertEquals("user failure", failures.get(0).getMessage());
        org.junit.jupiter.api.Assertions.assertTrue(failures.get(0).getSuppressed().length > 0);
    }


    @Test
    public void coversSqlRowsNoSqlLegacyAndInheritance() {
        assertTrue(execute(SqlRowsSample.class).isEmpty());
        assertEquals(1, execute(FailingSqlRowsSample.class).size());
        assertEquals(1, execute(NoSqlSample.class).size());
        assertTrue(execute(LegacyExpectationSample.class).isEmpty());
        assertTrue(execute(InheritedSqlSample.class).isEmpty());
        assertTrue(execute(MethodOverridesClassSample.class).isEmpty());
    }

    @Test
    public void coversSocketsDisableSocketsAndRegistryRestoration() {
        ConnectionsRegistry.INSTANCE.clear();
        assertTrue(execute(SocketSuccessSample.class).isEmpty());
        assertEquals(1, execute(SocketFailureSample.class).size());
        assertEquals(1, execute(NoSocketsSample.class).size());
        ConnectionsRegistry.INSTANCE.clear();
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("before", 123, 7);
        Map<Map.Entry<String, Integer>, Integer> before = new LinkedHashMap<Map.Entry<String, Integer>, Integer>(ConnectionsRegistry.INSTANCE.getDiscoveredAddresses());
        assertTrue(execute(DisableSocketsSample.class).isEmpty());
        assertEquals(before, new LinkedHashMap<Map.Entry<String, Integer>, Integer>(ConnectionsRegistry.INSTANCE.getDiscoveredAddresses()));
        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void sqlOnlyInvocationDoesNotTouchRegistryFlagsOrEntries() {
        ConnectionsRegistry.INSTANCE.clear();
        ConnectionsRegistry.INSTANCE.setPersistRegistry(true);
        ConnectionsRegistry.INSTANCE.setThreadLocal(false);
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("before", 123, 7);
        ConnectionsRegistry.INSTANCE.setDataSourceStatus("jdbc:before", "sa", 3);
        Map<Map.Entry<String, Integer>, Integer> addresses = new LinkedHashMap<Map.Entry<String, Integer>, Integer>(ConnectionsRegistry.INSTANCE.getDiscoveredAddresses());
        Map<Map.Entry<String, String>, Integer> dataSources = new LinkedHashMap<Map.Entry<String, String>, Integer>(ConnectionsRegistry.INSTANCE.getDiscoveredDataSources());
        assertTrue(execute(SqlRowsSample.class).isEmpty());
        assertTrue(ConnectionsRegistry.INSTANCE.isPersistRegistry());
        assertFalse(ConnectionsRegistry.INSTANCE.isThreadLocal());
        assertEquals(Integer.valueOf(7), ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().get(new AbstractMap.SimpleEntry<String, Integer>("before", 123)));
        assertEquals(Integer.valueOf(3), ConnectionsRegistry.INSTANCE.getDiscoveredDataSources().get(new AbstractMap.SimpleEntry<String, String>("jdbc:before", "sa")));
        assertTrue(ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().entrySet().containsAll(addresses.entrySet()));
        assertTrue(ConnectionsRegistry.INSTANCE.getDiscoveredDataSources().entrySet().containsAll(dataSources.entrySet()));
        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void disableSocketsRestoresPreExistingLiveConnectionAndFlags() {
        ConnectionsRegistry.INSTANCE.clear();
        ConnectionsRegistry.INSTANCE.setPersistRegistry(true);
        ConnectionsRegistry.INSTANCE.setThreadLocal(false);
        TrackingConnection connection = new TrackingConnection("localhost", 123);
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(connection.getInetSocketAddress(), connection);
        connection.status = 5;
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 123, 5);
        assertTrue(execute(DisableSocketsSample.class).isEmpty());
        assertTrue(ConnectionsRegistry.INSTANCE.isPersistRegistry());
        assertFalse(ConnectionsRegistry.INSTANCE.isThreadLocal());
        assertEquals(5, connection.status);
        connection.status = 0;
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 123, 6);
        assertEquals(6, connection.status);
        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void cleanupFailureAfterResourceAcquisitionIsSuppressedOnPrimaryFailure() {
        ConnectionsRegistry.INSTANCE.clear();
        TrackingConnection connection = new TrackingConnection("localhost", 124);
        connection.throwOnStatusCall = 3;
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(connection.getInetSocketAddress(), connection);
        List<Throwable> failures = execute(DisableSocketsUserFailureWithCleanupFailureSample.class);
        assertEquals(1, failures.size());
        assertEquals("user after resource acquisition", failures.get(0).getMessage());
        assertEquals(1, failures.get(0).getSuppressed().length);
        assertEquals("status cleanup failure", failures.get(0).getSuppressed()[0].getMessage());
        ConnectionsRegistry.INSTANCE.clear();
    }


    @Test
    public void disableSocketsDoesNotPersistTemporaryWildcardAndRestoresBackingFile() throws Exception {
        ConnectionsRegistry.INSTANCE.clear();
        File file = connectionsRegistryFile();
        file.delete();
        ConnectionsRegistry.INSTANCE.setPersistRegistry(true);
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("persisted", 321, 4);
        String before = read(file);
        assertTrue(before.contains("\"host\":\"persisted\""));
        assertFalse(before.contains("{,\"status\":-1}"));

        assertTrue(execute(DisableSocketsSample.class).isEmpty());
        assertEquals(before, read(file));

        SetupFailureAfterResourcesSample.executed = false;
        TrackingConnection connection = new TrackingConnection("localhost", 321);
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(connection.getInetSocketAddress(), connection);
        String beforeFailure = read(file);
        connection.throwOnEndpointlessStatus = true;
        connection.throwOnStatusCall = connection.statusCalls + 1;
        List<Throwable> failures = execute(SetupFailureAfterResourcesSample.class);
        assertEquals(1, failures.size());
        assertEquals("status cleanup failure", failures.get(0).getMessage());
        assertFalse(SetupFailureAfterResourcesSample.executed);
        assertEquals(beforeFailure, read(file));
        ConnectionsRegistry.INSTANCE.clear();
        file.delete();
    }

    @Test
    public void setupFailureAfterSpyAndSnapshotClosesSpyAndRestoresConnectivity() throws Exception {
        ConnectionsRegistry.INSTANCE.clear();
        ConnectionsRegistry.INSTANCE.setPersistRegistry(true);
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("setup", 125, 7);
        SetupFailureAfterResourcesSample.executed = false;
        TrackingConnection connection = new TrackingConnection("localhost", 125);
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(connection.getInetSocketAddress(), connection);
        String before = read(connectionsRegistryFile());
        connection.throwOnEndpointlessStatus = true;
        connection.throwOnStatusCall = connection.statusCalls + 1;

        List<Throwable> failures = execute(SetupFailureAfterResourcesSample.class);

        assertEquals(1, failures.size());
        assertEquals("status cleanup failure", failures.get(0).getMessage());
        assertEquals(1, failures.get(0).getSuppressed().length);
        assertTrue(failures.get(0).getSuppressed()[0].getClass().getName().contains("WrongNumberOfQueries"));
        assertFalse(SetupFailureAfterResourcesSample.executed);
        assertEquals(Integer.valueOf(7), ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().get(new AbstractMap.SimpleEntry<String, Integer>("setup", 125)));
        assertEquals(before, read(connectionsRegistryFile()));
        ConnectionsRegistry.INSTANCE.clear();
        connectionsRegistryFile().delete();
    }

    @Test
    public void bestEffortRestoreContinuesAfterFirstConnectionCallbackFailure() {
        ConnectionsRegistry.INSTANCE.clear();
        TrackingConnection first = new TrackingConnection("localhost", 126);
        TrackingConnection second = new TrackingConnection("localhost", 127);
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(first.getInetSocketAddress(), first);
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(second.getInetSocketAddress(), second);
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 126, 5);
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 127, 6);
        first.throwOnStatusCall = first.statusCalls + 2;
        second.throwOnStatusCall = 0;

        List<Throwable> failures = execute(DisableSocketsUserFailureWithCleanupFailureSample.class);

        assertEquals(1, failures.size());
        assertEquals("user after resource acquisition", failures.get(0).getMessage());
        assertTrue(first.statusCalls >= 4);
        assertEquals(1, failures.get(0).getSuppressed().length);
        assertEquals("status cleanup failure", failures.get(0).getSuppressed()[0].getMessage());
        assertEquals(6, second.status);
        second.status = 0;
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 127, 9);
        assertEquals(9, second.status);
        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void restoresRegistryAfterCombinedFailureAndSuppressesExactSniffyFailure() {
        ConnectionsRegistry.INSTANCE.clear();
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("before", 123, 7);
        List<Throwable> failures = execute(CombinedFailureSample.class);
        assertEquals(1, failures.size());
        assertEquals("user failure", failures.get(0).getMessage());
        assertEquals(1, failures.get(0).getSuppressed().length);
        assertTrue(failures.get(0).getSuppressed()[0].getClass().getName().contains("WrongNumberOfQueries"));
        assertTrue(ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().containsKey(new AbstractMap.SimpleEntry<String, Integer>("before", 123)));
        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void setupFailureRestoresRegistry() {
        ConnectionsRegistry.INSTANCE.clear();
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("before", 123, 7);
        List<Throwable> failures = execute(InvalidRangeSetupFailureSample.class);
        assertEquals(1, failures.size());
        assertTrue(ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().containsKey(new AbstractMap.SimpleEntry<String, Integer>("before", 123)));
        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void invalidAnnotationsPreventBodyExecution() {
        InvalidAnnotationSample.executed = false;
        List<Throwable> failures = execute(InvalidAnnotationSample.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, failures.size());
        org.junit.jupiter.api.Assertions.assertFalse(InvalidAnnotationSample.executed);
    }

    static List<Throwable> execute(Class<?> testClass) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(testClass)).build();
        Launcher launcher = LauncherFactory.create();
        final List<Throwable> failures = new ArrayList<Throwable>();
        launcher.execute(request, new TestExecutionListener() {
            public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                if (testIdentifier.isTest() && testExecutionResult.getThrowable().isPresent()) {
                    failures.add(testExecutionResult.getThrowable().get());
                }
            }
        });
        return failures;
    }

    public static class UnregisteredNoSqlSample {
        @Test
        @NoSql
        public void notRegistered() throws Exception {
            SniffyExtensionTest.query();
        }
    }

    @ExtendWith(SniffyExtension.class)
    public static class CombinedFailureSample {
        @Test
        @SqlExpectation(count = @io.sniffy.test.Count(1))
        public void failsBeforeSniffyValidation() {
            throw new IllegalStateException("user failure");
        }
    }

    @ExtendWith(SniffyExtension.class)
    public static class InvalidAnnotationSample {
        static boolean executed;
        @Test
        @NoSql
        @SqlExpectation(count = @io.sniffy.test.Count(1))
        public void invalid() {
            executed = true;
        }
    }
    @ExtendWith(SniffyExtension.class)
    public static class SqlRowsSample {
        @Test
        @SqlExpectation(count = @io.sniffy.test.Count(1), rows = @io.sniffy.test.Count(1))
        public void rows() throws Exception { SniffyExtensionTest.queryRows(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class FailingSqlRowsSample {
        @Test
        @SqlExpectation(count = @io.sniffy.test.Count(1), rows = @io.sniffy.test.Count(2))
        public void rows() throws Exception { SniffyExtensionTest.queryRows(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class NoSqlSample {
        @Test
        @NoSql
        public void noSql() throws Exception { SniffyExtensionTest.query(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class LegacyExpectationSample {
        @Test
        @Expectation(1)
        public void legacy() throws Exception { SniffyExtensionTest.query(); }
    }

    @SqlExpectation(count = @io.sniffy.test.Count(1))
    public static class ParentSqlSample {
    }

    @ExtendWith(SniffyExtension.class)
    public static class InheritedSqlSample extends ParentSqlSample {
        @Test
        public void inherited() throws Exception { SniffyExtensionTest.query(); }
    }

    @ExtendWith(SniffyExtension.class)
    @NoSql
    public static class MethodOverridesClassSample {
        @Test
        @SqlExpectation(count = @io.sniffy.test.Count(1))
        public void methodWins() throws Exception { SniffyExtensionTest.query(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class SocketSuccessSample {
        @Test
        @SocketExpectation(connections = @io.sniffy.test.Count(1))
        public void socket() throws Throwable { socketOperation(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class SocketFailureSample {
        @Test
        @SocketExpectation(connections = @io.sniffy.test.Count(2))
        public void socket() throws Throwable { socketOperation(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class NoSocketsSample {
        @Test
        @NoSocketsAllowed
        public void socket() throws Throwable { socketOperation(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class DisableSocketsSample {
        @Test
        @DisableSockets
        public void socketDisabled() throws Throwable {
            EchoServerRule echoServerRule = startServer();
            try {
                try {
                    new Socket(InetAddress.getByName(null), echoServerRule.getBoundPort());
                    fail("Connection should have been refused by Sniffy");
                } catch (ConnectException e) {
                    assertTrue(e.getMessage().contains("refused by Sniffy"));
                }
            } finally {
                echoServerRule.after();
            }
        }
    }

    @ExtendWith(SniffyExtension.class)
    public static class InvalidRangeSetupFailureSample {
        @Test
        @SqlExpectation(count = @io.sniffy.test.Count(min = 3, max = 1))
        public void invalid() throws Exception { SniffyExtensionTest.query(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class SetupFailureAfterResourcesSample {
        static boolean executed;
        @Test
        @DisableSockets
        @SqlExpectation(count = @io.sniffy.test.Count(1))
        public void setupFailsAfterResources() throws Exception {
            executed = true;
            SniffyExtensionTest.query();
        }
    }

    @ExtendWith(SniffyExtension.class)
    public static class DisableSocketsUserFailureWithCleanupFailureSample {
        @Test
        @DisableSockets
        @SqlExpectation(count = @io.sniffy.test.Count(0))
        public void failsAfterResourceAcquisition() { throw new IllegalStateException("user after resource acquisition"); }
    }

    static void socketOperation() throws Throwable {
        EchoServerRule echoServerRule = startServer();
        try {
            Socket socket = new Socket(InetAddress.getByName(null), echoServerRule.getBoundPort());
            socket.close();
            echoServerRule.joinThreads();
        } finally {
            echoServerRule.after();
        }
    }

    static EchoServerRule startServer() throws Throwable {
        EchoServerRule echoServerRule = new EchoServerRule(new byte[] {9, 8, 7});
        echoServerRule.before();
        return echoServerRule;
    }

    static File connectionsRegistryFile() {
        return new File(IOUtil.getApplicationSniffyFolder(), "connectionsRegistry.json");
    }

    static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    static class TrackingConnection implements SniffyNetworkConnection {
        private final InetSocketAddress address;
        int status;
        int statusCalls;
        int throwOnStatusCall;
        boolean throwOnEndpointlessStatus;

        TrackingConnection(String host, int port) {
            this.address = new InetSocketAddress(host, port);
        }

        public InetSocketAddress getInetSocketAddress() { return address; }
        public void setConnectionStatus(Integer connectionStatus) {
            setConnectionStatus(null, connectionStatus, true);
        }
        public void setConnectionStatus(InetSocketAddress endpoint, Integer connectionStatus) {
            setConnectionStatus(endpoint, connectionStatus, false);
        }
        private void setConnectionStatus(InetSocketAddress endpoint, Integer connectionStatus, boolean endpointless) {
            statusCalls++;
            if (statusCalls == throwOnStatusCall && (!throwOnEndpointlessStatus || endpointless)) throw new IllegalStateException("status cleanup failure");
            status = connectionStatus == null ? 0 : connectionStatus;
        }
        public void setProxiedInetSocketAddress(InetSocketAddress proxiedAddress) {}
        public InetSocketAddress getProxiedInetSocketAddress() { return null; }
        public void setFirstPacketSent(boolean firstPacketSent) {}
        public boolean isFirstPacketSent() { return false; }
        public int getPotentiallyBufferedInputBytes() { return 0; }
        public void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes) {}
        public int getPotentiallyBufferedOutputBytes() { return 0; }
        public void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes) {}
        public long getLastReadThreadId() { return 0; }
        public void setLastReadThreadId(long lastReadThreadId) {}
        public long getLastWriteThreadId() { return 0; }
        public void setLastWriteThreadId(long lastWriteThreadId) {}
        public void logSocket(long millis) {}
        public void logSocket(long millis, int bytesDown, int bytesUp) {}
        public void checkConnectionAllowed() throws ConnectException {}
        public void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException {}
        public void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException {}
        public void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException {}
        public void close() throws IOException {}
        public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {}
        public void logDecryptedTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {}
    }

}
