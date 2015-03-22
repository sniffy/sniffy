package com.github.bedrin.jdbc.sniffer;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ExpectedQueries implements Closeable {

    private final Sniffer sniffer;

    private final int initialQueries;
    private final int initialThreadLocalQueries;
    private final int initialOtherThreadsQueries;

    private final static Method addSuppressedMethod = getAddSuppressedMethod();

    private static Method getAddSuppressedMethod() {
        try {
            Class<Throwable> throwableClass = Throwable.class;
            return throwableClass.getMethod("addSuppressed", Throwable.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public ExpectedQueries(Sniffer sniffer) {
        this(sniffer, Sniffer.totalExecutedStatements(), ThreadLocalSniffer.totalExecutedStatements(), OtherThreadsSniffer.executedStatements());
    }

    public ExpectedQueries(Sniffer sniffer, int initialQueries, int initialThreadLocalQueries, int initialOtherThreadsQueries) {
        this.sniffer = sniffer;
        this.initialQueries = initialQueries;
        this.initialThreadLocalQueries = initialThreadLocalQueries;
        this.initialOtherThreadsQueries = initialOtherThreadsQueries;
    }

    public int executedStatements() {
        return sniffer.totalExecutedStatementsImpl() - initialQueries;
    }

    public int executedThreadLocalStatements() {
        return ThreadLocalSniffer.totalExecutedStatements() - initialThreadLocalQueries;
        //return sniffer.totalExecutedStatementsImpl() - initialThreadLocalQueries;
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

    public RecordedQueries run(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            try {
                verify();
            } catch (AssertionError ae) {
                if (!addSuppressed(e,ae)) {
                    ae.printStackTrace();
                }
            }
            ExpectedQueries.<RuntimeException>throwAny(e);
        }

        verify();
        return new RecordedQueries(
                sniffer.totalExecutedStatementsImpl() - initialQueries,
                ThreadLocalSniffer.totalExecutedStatements() - initialThreadLocalQueries,
                OtherThreadsSniffer.executedStatements() - initialThreadLocalQueries
        );
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }

    private static boolean addSuppressed(Throwable e, Throwable suppressed) {
        if (null == addSuppressedMethod) {
            return false;
        } else {
            try {
                addSuppressedMethod.invoke(e, suppressed);
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
            return true;
        }
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
            int numQueries = sniffer.totalExecutedStatementsImpl() - initialQueries;
            if (numQueries > maximumQueries || numQueries < minimumQueries)
                throw new AssertionError(String.format(
                        "Disallowed number of executed statements; expected between %d and %d; observed %d",
                        minimumQueries, maximumQueries, numQueries
                ));
        }

    }

}
