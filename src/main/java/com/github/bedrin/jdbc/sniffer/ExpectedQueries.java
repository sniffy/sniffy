package com.github.bedrin.jdbc.sniffer;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.addSuppressed;
import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.throwException;

public class ExpectedQueries<C extends ExpectedQueries<C>> implements Closeable {

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

    public C expectNoMoreQueries() {
        expectations.add(new AllThreadsExpectation(0, 0));
        return self();
    }

    public C verifyNoMoreQueries() {
        new AllThreadsExpectation(0, 0).validate();
        return self();
    }

    public C expectNotMoreThanOne() {
        expectations.add(new AllThreadsExpectation(0, 1));
        return self();
    }

    public C verifyNotMoreThanOne() {
        new AllThreadsExpectation(0, 1).validate();
        return self();
    }

    public C expectNotMoreThan(int allowedStatements) {
        expectations.add(new AllThreadsExpectation(0, allowedStatements));
        return self();
    }

    public C verifyNotMoreThan(int allowedStatements) {
        new AllThreadsExpectation(0, allowedStatements).validate();
        return self();
    }

    public C expectExact(int allowedStatements) {
        expectations.add(new AllThreadsExpectation(allowedStatements, allowedStatements));
        return self();
    }

    public C verifyExact(int allowedStatements) {
        new AllThreadsExpectation(allowedStatements, allowedStatements).validate();
        return self();
    }

    public C expectNotLessThan(int allowedStatements) {
        expectations.add(new AllThreadsExpectation(allowedStatements, Integer.MAX_VALUE));
        return self();
    }

    public C verifyNotLessThan(int allowedStatements) {
        new AllThreadsExpectation(allowedStatements, Integer.MAX_VALUE).validate();
        return self();
    }

    public C expectRange(int minAllowedStatements, int maxAllowedStatements) {
        expectations.add(new AllThreadsExpectation(minAllowedStatements, maxAllowedStatements));
        return self();
    }

    public C verifyRange(int minAllowedStatements, int maxAllowedStatements) {
        new AllThreadsExpectation(minAllowedStatements, maxAllowedStatements).validate();
        return self();
    }

    public C expectNoMoreThreadLocalQueries() {
        expectations.add(new ThreadLocalExpectation(0, 0));
        return self();
    }

    public C verifyNoMoreThreadLocalQueries() {
        new ThreadLocalExpectation(0, 0).validate();
        return self();
    }

    public C expectNotMoreThanOneThreadLocal() {
        expectations.add(new ThreadLocalExpectation(0, 1));
        return self();
    }

    public C verifyNotMoreThanOneThreadLocal() {
        new ThreadLocalExpectation(0, 1).validate();
        return self();
    }

    public C expectNotMoreThanThreadLocal(int allowedStatements) {
        expectations.add(new ThreadLocalExpectation(0, allowedStatements));
        return self();
    }

    public C verifyNotMoreThanThreadLocal(int allowedStatements) {
        new ThreadLocalExpectation(0, allowedStatements).validate();
        return self();
    }

    public C expectExactThreadLocal(int allowedStatements) {
        expectations.add(new ThreadLocalExpectation(allowedStatements, allowedStatements));
        return self();
    }

    public C verifyExactThreadLocal(int allowedStatements) {
        new ThreadLocalExpectation(allowedStatements, allowedStatements).validate();
        return self();
    }

    public C expectNotLessThanThreadLocal(int allowedStatements) {
        expectations.add(new ThreadLocalExpectation(allowedStatements, Integer.MAX_VALUE));
        return self();
    }

    public C verifyNotLessThanThreadLocal(int allowedStatements) {
        new ThreadLocalExpectation(allowedStatements, Integer.MAX_VALUE).validate();
        return self();
    }

    public C expectRangeThreadLocal(int minAllowedStatements, int maxAllowedStatements) {
        expectations.add(new ThreadLocalExpectation(minAllowedStatements, maxAllowedStatements));
        return self();
    }

    public C verifyRangeThreadLocal(int minAllowedStatements, int maxAllowedStatements) {
        new ThreadLocalExpectation(minAllowedStatements, maxAllowedStatements).validate();
        return self();
    }

    public C expectNoMoreOtherThreadsQueries() {
        expectations.add(new OtherThreadsExpectation(0, 0));
        return self();
    }

    public C verifyNoMoreOtherThreadsQueries() {
        new OtherThreadsExpectation(0, 0).validate();
        return self();
    }

    public C expectNotMoreThanOtherThreads() {
        expectations.add(new OtherThreadsExpectation(0, 1));
        return self();
    }

    public C verifyNotMoreThanOneOtherThreads() {
        new OtherThreadsExpectation(0, 1).validate();
        return self();
    }

    public C expectNotMoreThanOtherThreads(int allowedStatements) {
        expectations.add(new OtherThreadsExpectation(0, allowedStatements));
        return self();
    }

    public C verifyNotMoreThanOtherThreads(int allowedStatements) {
        new OtherThreadsExpectation(0, allowedStatements).validate();
        return self();
    }

    public C expectExactOtherThreads(int allowedStatements) {
        expectations.add(new OtherThreadsExpectation(allowedStatements, allowedStatements));
        return self();
    }

    public C verifyExactOtherThreads(int allowedStatements) {
        new OtherThreadsExpectation(allowedStatements, allowedStatements).validate();
        return self();
    }

    public C expectNotLessThanOtherThreads(int allowedStatements) {
        expectations.add(new OtherThreadsExpectation(allowedStatements, Integer.MAX_VALUE));
        return self();
    }

    public C verifyNotLessThanOtherThreads(int allowedStatements) {
        new OtherThreadsExpectation(allowedStatements, Integer.MAX_VALUE).validate();
        return self();
    }

    public C expectRangeOtherThreads(int minAllowedStatements, int maxAllowedStatements) {
        expectations.add(new OtherThreadsExpectation(minAllowedStatements, maxAllowedStatements));
        return self();
    }

    public C verifyRangeOtherThreads(int minAllowedStatements, int maxAllowedStatements) {
        new OtherThreadsExpectation(minAllowedStatements, maxAllowedStatements).validate();
        return self();
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

    public C execute(Sniffer.Executable executable) {
        try {
            executable.execute();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return self();
    }

    public C run(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return self();
    }

    public <T> RecordedQueriesWithValue<T> call(Callable<T> callable) {
        T result;

        try {
            result = callable.call();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return new RecordedQueriesWithValue<T>(result);
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

    private interface Expectation {
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

    private class ThreadLocalExpectation implements Expectation {

        private final int minimumQueries;
        private final int maximumQueries;

        public ThreadLocalExpectation(int minimumQueries, int maximumQueries) {
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
        }

        @Override
        public void validate() throws AssertionError {
            int numQueries = ThreadLocalSniffer.executedStatements(false) - initialThreadLocalQueries;
            if (numQueries > maximumQueries || numQueries < minimumQueries)
                throw new AssertionError(String.format(
                        "Disallowed number of executed statements; expected between %d and %d; observed %d",
                        minimumQueries, maximumQueries, numQueries
                ));
        }

    }

    private class OtherThreadsExpectation implements Expectation {

        private final int minimumQueries;
        private final int maximumQueries;

        public OtherThreadsExpectation(int minimumQueries, int maximumQueries) {
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
        }

        @Override
        public void validate() throws AssertionError {
            int numQueries = OtherThreadsSniffer.executedStatements(false) - initialQueries + initialThreadLocalQueries;
            if (numQueries > maximumQueries || numQueries < minimumQueries)
                throw new AssertionError(String.format(
                        "Disallowed number of executed statements; expected between %d and %d; observed %d",
                        minimumQueries, maximumQueries, numQueries
                ));
        }

    }

    @SuppressWarnings("unchecked")
    private C self() {
        return (C) this;
    }

}
