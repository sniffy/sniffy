package com.github.bedrin.jdbc.sniffer;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.addSuppressed;
import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.throwException;

public class ExpectedQueries implements Closeable {

    private final int initialQueries;
    private final int initialThreadLocalQueries;

    public ExpectedQueries() {
        this(Sniffer.executedStatements(false), ThreadLocalSniffer.executedStatements(false));
    }

    public ExpectedQueries(int initialQueries, int initialThreadLocalQueries) {
        this.initialQueries = initialQueries;
        this.initialThreadLocalQueries = initialThreadLocalQueries;
    }

    private List<Expectation> expectations = new ArrayList<Expectation>();

    public ExpectedQueries expectNoMoreQueries() {
        expectations.add(new AllThreadsExpectation(0, 0));
        return this;
    }

    public ExpectedQueries verifyNoMoreQueries() {
        new AllThreadsExpectation(0, 0).validate();
        return this;
    }

    public ExpectedQueries expectNotMoreThanOne() {
        expectations.add(new AllThreadsExpectation(0, 1));
        return this;
    }

    public ExpectedQueries verifyNotMoreThanOne() {
        new AllThreadsExpectation(0, 1).validate();
        return this;
    }

    public ExpectedQueries expectNotMoreThan(int allowedStatements) {
        expectations.add(new AllThreadsExpectation(0, allowedStatements));
        return this;
    }

    public ExpectedQueries verifyNotMoreThan(int allowedStatements) {
        new AllThreadsExpectation(0, allowedStatements).validate();
        return this;
    }

    public ExpectedQueries expectExact(int allowedStatements) {
        expectations.add(new AllThreadsExpectation(allowedStatements, allowedStatements));
        return this;
    }

    public ExpectedQueries verifyExact(int allowedStatements) {
        new AllThreadsExpectation(allowedStatements, allowedStatements).validate();
        return this;
    }

    public ExpectedQueries expectNotLessThan(int allowedStatements) {
        expectations.add(new AllThreadsExpectation(allowedStatements, Integer.MAX_VALUE));
        return this;
    }

    public ExpectedQueries verifyNotLessThan(int allowedStatements) {
        new AllThreadsExpectation(allowedStatements, Integer.MAX_VALUE).validate();
        return this;
    }

    public ExpectedQueries expectRange(int minAllowedStatements, int maxAllowedStatements) {
        expectations.add(new AllThreadsExpectation(minAllowedStatements, maxAllowedStatements));
        return this;
    }

    public ExpectedQueries verifyRange(int minAllowedStatements, int maxAllowedStatements) {
        new AllThreadsExpectation(minAllowedStatements, maxAllowedStatements).validate();
        return this;
    }

    public void verify() throws AssertionError {
        AssertionError assertionError = null;
        Throwable currentException = null;
        for (Expectation expectation : expectations) {
            try {
                expectation.validate();
            } catch (AssertionError e) {
                if (null == assertionError) {
                    currentException = assertionError = e;
                } else {
                    currentException.initCause(e);
                    currentException = e;
                }
            }
        }
        if (null != assertionError) {
            throw assertionError;
        }
    }

    @Override
    public void close() {
        verify();
    }

    public RecordedQueries execute(Sniffer.Executable executable) {
        try {
            executable.execute();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return new RecordedQueries(
                Sniffer.executedStatements(false) - initialQueries,
                ThreadLocalSniffer.executedStatements(false) - initialThreadLocalQueries,
                OtherThreadsSniffer.executedStatements(false) - initialQueries + initialThreadLocalQueries
        );
    }

    public RecordedQueries run(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return new RecordedQueries(
                Sniffer.executedStatements(false) - initialQueries,
                ThreadLocalSniffer.executedStatements(false) - initialThreadLocalQueries,
                OtherThreadsSniffer.executedStatements(false) - initialQueries + initialThreadLocalQueries
        );
    }

    public <T> RecordedQueriesWithValue<T> call(Callable<T> callable) {
        T result;

        try {
            result = callable.call();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return new RecordedQueriesWithValue<T>(
                result,
                Sniffer.executedStatements(false) - initialQueries,
                ThreadLocalSniffer.executedStatements(false) - initialThreadLocalQueries,
                OtherThreadsSniffer.executedStatements(false) - initialQueries + initialThreadLocalQueries
        );
    }

    private RuntimeException verifyAndAddToException(Throwable e) {
        try {
            verify();
        } catch (AssertionError ae) {
            if (!addSuppressed(e, ae)) {
                ae.printStackTrace();
            }
        }
        throwException(e);
        return new RuntimeException(e);
    }

    private static interface Expectation {
        void validate() throws AssertionError;
    }

    private class AllThreadsExpectation implements Expectation {

        private final int minimumQueries;
        private final int maximumQueries;

        public AllThreadsExpectation(int minimumQueries, int maximumQueries) {
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
        }

        @Override
        public void validate() throws AssertionError {
            int numQueries = Sniffer.executedStatements(false) - initialQueries;
            if (numQueries > maximumQueries || numQueries < minimumQueries)
                throw new AssertionError(String.format(
                        "Disallowed number of executed statements; expected between %d and %d; observed %d",
                        minimumQueries, maximumQueries, numQueries
                ));
        }

    }

}
