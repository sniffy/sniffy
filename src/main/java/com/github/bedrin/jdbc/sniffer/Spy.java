package com.github.bedrin.jdbc.sniffer;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.github.bedrin.jdbc.sniffer.Sniffer.DEFAULT_THREAD_MATCHER;
import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.addSuppressed;
import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.throwException;

/**
 * Spy holds a number of queries which were executed at some point of time and uses it as a base for further assertions
 * @see Sniffer#spy()
 * @see Sniffer#expect(int)
 * @since 2.0
 */
public class Spy<C extends Spy<C>> implements Closeable {

    private int initialQueries;
    private int initialThreadLocalQueries;

    /**
     * @since 2.0
     */
    public Spy() {
        this(Sniffer.executedStatements(), Sniffer.ThreadLocalSniffer.executedStatements());
    }

    /**
     * @param initialQueries total number of queries executed since some point of time
     * @param initialThreadLocalQueries total number of queries executed by current thread since some point of time
     * @since 2.0
     */
    public Spy(int initialQueries, int initialThreadLocalQueries) {
        this.initialQueries = initialQueries;
        this.initialThreadLocalQueries = initialThreadLocalQueries;
    }

    private List<Expectation> expectations = new ArrayList<Expectation>();

    /**
     * Wrapper for {@link Sniffer#spy()} method; useful for chaining
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    public Spy reset() {
        this.initialQueries = Sniffer.executedStatements();
        this.initialThreadLocalQueries =  Sniffer.ThreadLocalSniffer.executedStatements();
        return self();
    }

    /**
     * @return number of SQL statements executed by current thread since some fixed moment of time
     * @since 2.0
     */
    public int executedStatements() {
        return executedStatements(DEFAULT_THREAD_MATCHER);
    }

    /**
     * @param threadMatcher chooses {@link Thread}s for calculating the number of executed queries
     * @return number of SQL statements executed since some fixed moment of time
     * @since 2.0
     */
    public int executedStatements(Threads threadMatcher) {

        switch (threadMatcher) {
            case ANY:
                return Sniffer.executedStatements() - initialQueries;
            case CURRENT:
                return Sniffer.ThreadLocalSniffer.executedStatements() - initialThreadLocalQueries;
            case OTHERS:
                return Sniffer.executedStatements() - Sniffer.ThreadLocalSniffer.executedStatements()
                        - initialQueries + initialThreadLocalQueries;
            default:
                throw new IllegalArgumentException(String.format("Unknown thread matcher %s", threadMatcher.getClass().getName()));
        }

    }

    // never methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments 0, 0, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expectNever() {
        return expectNever(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments 0, 0, {@code threads}
     * @since 2.0
     */
    public C expectNever(Threads threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(0, 0, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments 0, 0, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verifyNever() throws WrongNumberOfQueriesError {
        return verifyNever(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments 0, 0, {@code threads}
     * @since 2.0
     */
    public C verifyNever(Threads threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(0, 0, threadMatcher).validate();
        return self();
    }

    // atMostOnce methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments 0, 1, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expectAtMostOnce() {
        return expectAtMostOnce(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments 0, 1, {@code threads}
     * @since 2.0
     */
    public C expectAtMostOnce(Threads threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(0, 1, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments 0, 1, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verifyAtMostOnce() throws WrongNumberOfQueriesError {
        return verifyAtMostOnce(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments 0, 1, {@code threads}
     * @since 2.0
     */
    public C verifyAtMostOnce(Threads threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(0, 1, threadMatcher).validate();
        return self();
    }

    // atMost methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments 0, {@code allowedStatements}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expectAtMost(int allowedStatements) {
        return expectAtMost(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments 0, {@code allowedStatements}, {@code threads}
     * @since 2.0
     */
    public C expectAtMost(int allowedStatements, Threads threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments 0, {@code allowedStatements}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verifyAtMost(int allowedStatements) throws WrongNumberOfQueriesError {
        return verifyAtMost(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments 0, {@code allowedStatements}, {@code threads}
     * @since 2.0
     */
    public C verifyAtMost(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(0, allowedStatements, threadMatcher).validate();
        return self();
    }

    // exact methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments {@code allowedStatements}, {@code allowedStatements}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expect(int allowedStatements) {
        return expect(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}
     * @since 2.0
     */
    public C expect(int allowedStatements, Threads threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments {@code allowedStatements}, {@code allowedStatements}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verify(int allowedStatements) throws WrongNumberOfQueriesError {
        return verify(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}
     * @since 2.0
     */
    public C verify(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher).validate();
        return self();
    }

    // atLeast methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expectAtLeast(int allowedStatements) {
        return expectAtLeast(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}
     * @since 2.0
     */
    public C expectAtLeast(int allowedStatements, Threads threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verifyAtLeast(int allowedStatements) throws WrongNumberOfQueriesError {
        return verifyAtLeast(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}
     * @since 2.0
     */
    public C verifyAtLeast(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher).validate();
        return self();
    }

    // between methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads)} with arguments {@code minAllowedStatements}, {@code maxAllowedStatements}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements) {
        return expectBetween(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.0
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments {@code minAllowedStatements}, {@code maxAllowedStatements}, {@code threads}
     * @since 2.0
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements) throws WrongNumberOfQueriesError {
        return verifyBetween(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher).validate();
        return self();
    }

    // end

    /**
     * Verifies all expectations added previously using {@code expect} methods family
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public void verify() throws WrongNumberOfQueriesError {
        WrongNumberOfQueriesError assertionError = getWrongNumberOfQueriesError();
        if (null != assertionError) {
            throw assertionError;
        }
    }

    /**
     *
     * @return WrongNumberOfQueriesError or null if there are no errors
     * @since 2.1
     */
    public WrongNumberOfQueriesError getWrongNumberOfQueriesError() {
        WrongNumberOfQueriesError assertionError = null;
        Throwable currentException = null;
        for (Expectation expectation : expectations) {
            try {
                expectation.validate();
            } catch (WrongNumberOfQueriesError e) {
                if (null == assertionError) {
                    currentException = assertionError = e;
                } else {
                    currentException.initCause(e);
                    currentException = e;
                }
            }
        }
        return assertionError;
    }

    /**
     * Alias for {@link #verify()} method; it is useful for try-with-resource API:
     * <pre>
     * <code>
     *     {@literal @}Test
     *     public void testTryWithResourceApi() throws SQLException {
     *         final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
     *         try (@SuppressWarnings("unused") Spy s = Sniffer.expectAtMostOnce();
     *              Statement statement = connection.createStatement()) {
     *             statement.execute("SELECT 1 FROM DUAL");
     *         }
     *     }
     * }
     * </code>
     * </pre>
     * @since 2.0
     */
    @Override
    public void close() {
        verify();
    }

    /**
     * Executes the {@link Sniffer.Executable#execute()} method on provided argument and verifies the expectations
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public C execute(Sniffer.Executable executable) {
        try {
            executable.execute();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return self();
    }

    /**
     * Executes the {@link Runnable#run()} method on provided argument and verifies the expectations
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public C run(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return self();
    }

    /**
     * Executes the {@link Callable#call()} method on provided argument and verifies the expectations
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public <V> SpyWithValue<V> call(Callable<V> callable) {
        V result;

        try {
            result = callable.call();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return new SpyWithValue<V>(result);
    }

    private RuntimeException verifyAndAddToException(Throwable e) {
        try {
            verify();
        } catch (WrongNumberOfQueriesError ae) {
            if (!addSuppressed(e, ae)) {
                ae.printStackTrace();
            }
        }
        throwException(e);
        return new RuntimeException(e);
    }

    private interface Expectation {

        void validate() throws WrongNumberOfQueriesError;

    }

    private class ThreadMatcherExpectation implements Expectation {

        private final int minimumQueries;
        private final int maximumQueries;
        private final Threads threadMatcher;

        public ThreadMatcherExpectation(int minimumQueries, int maximumQueries, Threads threadMatcher) {
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
            this.threadMatcher = threadMatcher;
        }

        @Override
        public void validate() throws WrongNumberOfQueriesError {

            switch (threadMatcher) {
                case ANY:
                {
                    int numQueries = Sniffer.executedStatements() - initialQueries;
                    if (numQueries > maximumQueries || numQueries < minimumQueries)
                        throw new WrongNumberOfQueriesError(String.format(
                                "Disallowed number of executed statements; expected between %d and %d; observed %d",
                                minimumQueries, maximumQueries, numQueries
                        ));
                }
                break;
                case CURRENT:
                {
                    int numQueries = Sniffer.ThreadLocalSniffer.executedStatements() - initialThreadLocalQueries;
                    if (numQueries > maximumQueries || numQueries < minimumQueries)
                        throw new WrongNumberOfQueriesError(String.format(
                                "Disallowed number of executed statements; expected between %d and %d; observed %d",
                                minimumQueries, maximumQueries, numQueries
                        ));
                }
                break;
                case OTHERS: {
                    int numQueries = Sniffer.executedStatements() - Sniffer.ThreadLocalSniffer.executedStatements()
                            - initialQueries + initialThreadLocalQueries;
                    if (numQueries > maximumQueries || numQueries < minimumQueries)
                        throw new WrongNumberOfQueriesError(String.format(
                                "Disallowed number of executed statements; expected between %d and %d; observed %d",
                                minimumQueries, maximumQueries, numQueries
                        ));
                }
                break;
            }

        }

    }

    @SuppressWarnings("unchecked")
    private C self() {
        return (C) this;
    }

}
