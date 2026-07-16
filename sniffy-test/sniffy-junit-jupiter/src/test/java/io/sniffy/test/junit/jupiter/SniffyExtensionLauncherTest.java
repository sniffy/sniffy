package io.sniffy.test.junit.jupiter;

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

import java.util.ArrayList;
import java.util.List;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

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
}
