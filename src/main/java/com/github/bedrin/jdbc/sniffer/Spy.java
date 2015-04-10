package com.github.bedrin.jdbc.sniffer;

import com.github.bedrin.jdbc.sniffer.Sniffer.ThreadMatcher;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.github.bedrin.jdbc.sniffer.Sniffer.DEFAULT_THREAD_MATCHER;
import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.addSuppressed;
import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.throwException;

/**
 * @since 2.0
 */
public class Spy<C extends Spy<C>> implements Closeable {

    private final int initialQueries;
    private final int initialThreadLocalQueries;

    public Spy() {
        this(Sniffer.executedStatements(), Sniffer.ThreadLocalSniffer.executedStatements());
    }

    public Spy(int initialQueries, int initialThreadLocalQueries) {
        this.initialQueries = initialQueries;
        this.initialThreadLocalQueries = initialThreadLocalQueries;
    }

    private List<Expectation> expectations = new ArrayList<Expectation>();

    /**
     * Wrapper for {@link Sniffer#spy()} method; useful for chaining
     * @return a new {@link Spy} instance
     */
    public Spy reset() {
        return Sniffer.spy();
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
    public int executedStatements(ThreadMatcher threadMatcher) {

        if (threadMatcher instanceof Sniffer.AnyThread) {
            return Sniffer.executedStatements() - initialQueries;
        } else if (threadMatcher instanceof Sniffer.CurrentThread) {
            return Sniffer.ThreadLocalSniffer.executedStatements() - initialThreadLocalQueries;
        } else if (threadMatcher instanceof Sniffer.OtherThreads) {
            return Sniffer.executedStatements() - Sniffer.ThreadLocalSniffer.executedStatements()
                    - initialQueries + initialThreadLocalQueries;
        } else {
            throw new IllegalArgumentException(String.format("Unknown thread matcher %s", threadMatcher.getClass().getName()));
        }

    }

    // noMore methods

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments 0, 0, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expectNever() {
        return expectNever(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments 0, 0, {@code threadMatcher}
     * @since 2.0
     */
    public C expectNever(ThreadMatcher threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(0, 0, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments 0, 0, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verifyNever() throws WrongNumberOfQueriesError {
        return verifyNever(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments 0, 0, {@code threadMatcher}
     * @since 2.0
     */
    public C verifyNever(ThreadMatcher threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(0, 0, threadMatcher).validate();
        return self();
    }

    // notMoreThanOne methods

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments 0, 1, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expectAtMostOnce() {
        return expectAtMostOnce(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments 0, 1, {@code threadMatcher}
     * @since 2.0
     */
    public C expectAtMostOnce(ThreadMatcher threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(0, 1, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments 0, 1, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verifyAtMostOnce() throws WrongNumberOfQueriesError {
        return verifyAtMostOnce(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments 0, 1, {@code threadMatcher}
     * @since 2.0
     */
    public C verifyAtMostOnce(ThreadMatcher threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(0, 1, threadMatcher).validate();
        return self();
    }

    // notMoreThan methods

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments 0, {@code allowedStatements}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expectAtMost(int allowedStatements) {
        return expectAtMost(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments 0, {@code allowedStatements}, {@code threadMatcher}
     * @since 2.0
     */
    public C expectAtMost(int allowedStatements, ThreadMatcher threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments 0, {@code allowedStatements}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verifyAtMost(int allowedStatements) throws WrongNumberOfQueriesError {
        return verifyAtMost(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments 0, {@code allowedStatements}, {@code threadMatcher}
     * @since 2.0
     */
    public C verifyAtMost(int allowedStatements, ThreadMatcher threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(0, allowedStatements, threadMatcher).validate();
        return self();
    }

    // exact methods

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@code allowedStatements}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expect(int allowedStatements) {
        return expect(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threadMatcher}
     * @since 2.0
     */
    public C expect(int allowedStatements, ThreadMatcher threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@code allowedStatements}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verify(int allowedStatements) throws WrongNumberOfQueriesError {
        return verify(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threadMatcher}
     * @since 2.0
     */
    public C verify(int allowedStatements, ThreadMatcher threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher).validate();
        return self();
    }

    // notLessThan methods

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C expectAtLeast(int allowedStatements) {
        return expectAtLeast(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threadMatcher}
     * @since 2.0
     */
    public C expectAtLeast(int allowedStatements, ThreadMatcher threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {#link Sniffer#CURRENT_THREAD}
     * @since 2.0
     */
    public C verifyAtLeast(int allowedStatements) throws WrongNumberOfQueriesError {
        return verifyAtLeast(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threadMatcher}
     * @since 2.0
     */
    public C verifyAtLeast(int allowedStatements, ThreadMatcher threadMatcher) throws WrongNumberOfQueriesError {
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher).validate();
        return self();
    }

    // range methods

    /**
     * Alias for {@link #expectBetween(int, int, ThreadMatcher)} with arguments {@code minAllowedStatements}, {@code maxAllowedStatements}, {#link Sniffer#CURRENT_THREAD}
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
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, ThreadMatcher threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments {@code minAllowedStatements}, {@code maxAllowedStatements}, {@code threadMatcher}
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
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, ThreadMatcher threadMatcher) throws WrongNumberOfQueriesError {
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
        if (null != assertionError) {
            throw assertionError;
        }
    }

    /**
     * Alias for {@link #verify()} method; it is useful for try-with-resource API:
     * <pre>
     * {@code
     *     @Test
     *     public void testTryWithResourceApi() throws SQLException {
     *         final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
     *         try (@SuppressWarnings("unused") Spy s = Sniffer.expectNotMoreThanOne();
     *              Statement statement = connection.createStatement()) {
     *             statement.execute("SELECT 1 FROM DUAL");
     *         }
     *     }
     * }
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
        private final ThreadMatcher threadMatcher;

        public ThreadMatcherExpectation(int minimumQueries, int maximumQueries, ThreadMatcher threadMatcher) {
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
            this.threadMatcher = threadMatcher;
        }

        @Override
        public void validate() throws WrongNumberOfQueriesError {

            if (threadMatcher instanceof Sniffer.AnyThread) {
                int numQueries = Sniffer.executedStatements() - initialQueries;
                if (numQueries > maximumQueries || numQueries < minimumQueries)
                    throw new WrongNumberOfQueriesError(String.format(
                            "Disallowed number of executed statements; expected between %d and %d; observed %d",
                            minimumQueries, maximumQueries, numQueries
                    ));
            } else if (threadMatcher instanceof Sniffer.CurrentThread) {
                int numQueries = Sniffer.ThreadLocalSniffer.executedStatements() - initialThreadLocalQueries;
                if (numQueries > maximumQueries || numQueries < minimumQueries)
                    throw new WrongNumberOfQueriesError(String.format(
                            "Disallowed number of executed statements; expected between %d and %d; observed %d",
                            minimumQueries, maximumQueries, numQueries
                    ));
            } else if (threadMatcher instanceof Sniffer.OtherThreads) {
                int numQueries = Sniffer.executedStatements() - Sniffer.ThreadLocalSniffer.executedStatements()
                        - initialQueries + initialThreadLocalQueries;
                if (numQueries > maximumQueries || numQueries < minimumQueries)
                    throw new WrongNumberOfQueriesError(String.format(
                            "Disallowed number of executed statements; expected between %d and %d; observed %d",
                            minimumQueries, maximumQueries, numQueries
                    ));
            } else {
                throw new WrongNumberOfQueriesError(String.format("Unknown thread matcher %s", threadMatcher.getClass().getName()));
            }

        }

    }

    @SuppressWarnings("unchecked")
    private C self() {
        return (C) this;
    }

}
