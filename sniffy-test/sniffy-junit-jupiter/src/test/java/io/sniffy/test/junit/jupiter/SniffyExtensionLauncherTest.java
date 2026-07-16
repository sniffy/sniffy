package io.sniffy.test.junit.jupiter;

import io.sniffy.Expectation;
import io.sniffy.Threads;
import io.sniffy.NoQueriesAllowed;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.DisableSockets;
import io.sniffy.socket.EchoServerRule;
import io.sniffy.socket.NoSocketsAllowed;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.SocketExpectations;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.socket.Protocol;
import io.sniffy.sql.NoSql;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlExpectations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void coversSqlRowsNoSqlLegacyContainersAndInheritance() {
        assertTrue(execute(SqlRowsSample.class).isEmpty());
        assertEquals(1, execute(FailingSqlRowsSample.class).size());
        assertEquals(1, execute(NoSqlSample.class).size());
        assertEquals(1, execute(NoQueriesAllowedSample.class).size());
        assertTrue(execute(LegacyExpectationSample.class).isEmpty());
        assertTrue(execute(LegacyExpectationsContainerSample.class).isEmpty());
        assertTrue(execute(SqlExpectationsContainerSample.class).isEmpty());
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
        assertTrue(execute(DisableSocketsSample.class).isEmpty());
        assertTrue(ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().isEmpty());
        ConnectionsRegistry.INSTANCE.clear();
    }


    @Test
    public void coversSocketContainerAnnotation() {
        ConnectionsRegistry.INSTANCE.clear();
        List<Throwable> socketContainerFailures = execute(SocketExpectationsContainerSample.class);
        assertTrue(socketContainerFailures.isEmpty(), socketContainerFailures.toString());
    }

    @Test
    public void monitorsSocketActivityInBeforeTestAndAfterEach() {
        ConnectionsRegistry.INSTANCE.clear();
        List<Throwable> socketLifecycleFailures = execute(SocketLifecycleSample.class);
        assertTrue(socketLifecycleFailures.isEmpty(), socketLifecycleFailures.toString());
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
    public void setupFailureAfterResourcesRollsBackWithoutExecutingBody() {
        ConnectionsRegistry.INSTANCE.clear();
        SetupFailureAfterResourcesSample.executed = false;
        SetupFailureAfterResourcesSample.connection = new ThrowingNetworkConnection();
        ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 12345),
                SetupFailureAfterResourcesSample.connection
        );
        List<Throwable> failures = execute(SetupFailureAfterResourcesSample.class);
        assertEquals(1, failures.size());
        assertEquals("injected setup failure", failures.get(0).getMessage());
        assertFalse(SetupFailureAfterResourcesSample.executed);
        assertTrue(ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().isEmpty());
        assertEquals(1, failures.get(0).getSuppressed().length);
        assertTrue(failures.get(0).getSuppressed()[0].getClass().getName().contains("WrongNumberOfQueries"));
        SetupFailureAfterResourcesSample.connection = null;
    }

    @Test
    public void monitorsSqlAcrossBeforeTestAndAllAfterEachMethods() {
        MultipleAfterEachLifecycleSample.afterInvocations.set(0);
        List<Throwable> failures = execute(MultipleAfterEachLifecycleSample.class);
        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals(2, MultipleAfterEachLifecycleSample.afterInvocations.get());
    }

    @Test
    public void disableSocketsCombinedFailureKeepsUserPrimaryAndCleansRegistry() {
        ConnectionsRegistry.INSTANCE.clear();
        List<Throwable> failures = execute(DisableSocketsUserFailureWithCleanupFailureSample.class);
        assertEquals(1, failures.size());
        assertEquals("user after resource acquisition", failures.get(0).getMessage());
        assertEquals(1, failures.get(0).getSuppressed().length);
        assertTrue(failures.get(0).getSuppressed()[0].getClass().getName().contains("WrongNumberOfQueries"));
        assertTrue(ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().isEmpty());
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
    public static class NoQueriesAllowedSample {
        @Test
        @NoQueriesAllowed
        public void noQueriesAllowed() throws Exception { SniffyExtensionTest.query(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class LegacyExpectationSample {
        @Test
        @Expectation(1)
        public void legacy() throws Exception { SniffyExtensionTest.query(); }
    }

    @ExtendWith(SniffyExtension.class)
    public static class LegacyExpectationsContainerSample {
        @Test
        @io.sniffy.Expectations({@Expectation(2), @Expectation(2)})
        public void legacyContainer() throws Exception {
            SniffyExtensionTest.query();
            SniffyExtensionTest.query();
        }
    }

    @ExtendWith(SniffyExtension.class)
    public static class SqlExpectationsContainerSample {
        @Test
        @SqlExpectations({@SqlExpectation(count = @io.sniffy.test.Count(2)), @SqlExpectation(count = @io.sniffy.test.Count(2))})
        public void sqlContainer() throws Exception {
            SniffyExtensionTest.query();
            SniffyExtensionTest.query();
        }
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
    public static class SocketExpectationsContainerSample {
        @Test
        @SocketExpectations({@SocketExpectation(connections = @io.sniffy.test.Count(min = 0), threads = Threads.ANY), @SocketExpectation(connections = @io.sniffy.test.Count(min = 0), threads = Threads.ANY)})
        public void socketContainer() throws Throwable {
            socketOperation();
        }
    }

    @ExtendWith(SniffyExtension.class)
    public static class SocketLifecycleSample {
        @BeforeEach
        public void before() throws Throwable { socketOperation(); }

        @Test
        @SocketExpectation(connections = @io.sniffy.test.Count(5), threads = Threads.ANY)
        public void socket() throws Throwable {
            socketOperation();
            socketOperation();
            socketOperation();
        }

        @AfterEach
        public void after() throws Throwable { socketOperation(); }
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
    public static class SetupFailureAfterResourcesSample {
        static boolean executed;
        static ThrowingNetworkConnection connection;

        @Test
        @DisableSockets
        @SqlExpectation(count = @io.sniffy.test.Count(1))
        public void setupFailsAfterResources() throws Exception {
            executed = true;
            SniffyExtensionTest.query();
        }
    }

    public static class ParentAfterEachLifecycleSample {
        @AfterEach
        public void parentAfter() throws Exception {
            MultipleAfterEachLifecycleSample.afterInvocations.incrementAndGet();
            SniffyExtensionTest.query();
        }
    }

    @ExtendWith(SniffyExtension.class)
    public static class MultipleAfterEachLifecycleSample extends ParentAfterEachLifecycleSample {
        static final AtomicInteger afterInvocations = new AtomicInteger();

        @BeforeEach
        public void before() throws Exception { SniffyExtensionTest.query(); }

        @Test
        @SqlExpectation(count = @io.sniffy.test.Count(4))
        public void test() throws Exception { SniffyExtensionTest.query(); }

        @AfterEach
        public void childAfter() throws Exception {
            afterInvocations.incrementAndGet();
            SniffyExtensionTest.query();
        }
    }

    @ExtendWith(SniffyExtension.class)
    public static class DisableSocketsUserFailureWithCleanupFailureSample {
        @Test
        @DisableSockets
        @SqlExpectation(count = @io.sniffy.test.Count(1))
        public void failsAfterResourceAcquisition() { throw new IllegalStateException("user after resource acquisition"); }
    }

    static class ThrowingNetworkConnection implements SniffyNetworkConnection {
        public InetSocketAddress getInetSocketAddress() { return new InetSocketAddress(InetAddress.getLoopbackAddress(), 12345); }
        public void setConnectionStatus(Integer connectionStatus) { if (Integer.valueOf(-1).equals(connectionStatus)) throw new IllegalStateException("injected setup failure"); }
        public void setProxiedInetSocketAddress(InetSocketAddress proxiedAddress) { }
        public InetSocketAddress getProxiedInetSocketAddress() { return null; }
        public void setFirstPacketSent(boolean firstPacketSent) { }
        public boolean isFirstPacketSent() { return false; }
        public int getPotentiallyBufferedInputBytes() { return 0; }
        public void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes) { }
        public int getPotentiallyBufferedOutputBytes() { return 0; }
        public void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes) { }
        public long getLastReadThreadId() { return 0; }
        public void setLastReadThreadId(long lastReadThreadId) { }
        public long getLastWriteThreadId() { return 0; }
        public void setLastWriteThreadId(long lastWriteThreadId) { }
        public void logSocket(long millis) { }
        public void logSocket(long millis, int bytesDown, int bytesUp) { }
        public void checkConnectionAllowed() throws ConnectException { }
        public void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException { }
        public void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException { }
        public void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException { }
        public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) { }
        public void logDecryptedTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) { }
    }

    static void socketOperation() throws Throwable {
        EchoServerRule echoServerRule = startServer();
        Socket socket = null;
        try {
            socket = new Socket(InetAddress.getByName(null), echoServerRule.getBoundPort());
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(new byte[] {1, 2, 3, 4});
            outputStream.flush();
            socket.shutdownOutput();
            InputStream inputStream = socket.getInputStream();
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            int read;
            while ((read = inputStream.read()) != -1) {
                response.write(read);
            }
            socket.shutdownInput();
            echoServerRule.joinThreads();
            assertArrayEquals(new byte[] {9, 8, 7}, response.toByteArray());
        } finally {
            if (socket != null) {
                socket.close();
            }
            echoServerRule.after();
        }
    }

    static EchoServerRule startServer() throws Throwable {
        EchoServerRule echoServerRule = new EchoServerRule(new byte[] {9, 8, 7});
        echoServerRule.before();
        return echoServerRule;
    }

}
