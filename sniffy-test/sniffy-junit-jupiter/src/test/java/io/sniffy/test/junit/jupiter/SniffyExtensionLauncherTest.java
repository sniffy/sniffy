package io.sniffy.test.junit.jupiter;

import io.sniffy.Expectation;
import io.sniffy.NoQueriesAllowed;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.DisableSockets;
import io.sniffy.socket.EchoServerRule;
import io.sniffy.socket.NoSocketsAllowed;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.TcpConnectionsExpectationError;
import io.sniffy.sql.NoSql;
import io.sniffy.sql.SqlExpectation;
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
import java.net.Socket;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("before", 123, 7);
        Map<Map.Entry<String, Integer>, Integer> before = ConnectionsRegistry.INSTANCE.getDiscoveredAddresses();
        assertTrue(execute(SocketSuccessSample.class).isEmpty());
        assertEquals(1, execute(SocketFailureSample.class).size());
        assertEquals(1, execute(NoSocketsSample.class).size());
        assertTrue(execute(DisableSocketsSample.class).isEmpty());
        assertEquals(before, ConnectionsRegistry.INSTANCE.getDiscoveredAddresses());
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

}
