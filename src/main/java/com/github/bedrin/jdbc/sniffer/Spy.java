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
            throw new AssertionError(String.format("Unknown thread matcher %s", threadMatcher.getClass().getName()));
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
    public C verifyNever() {
        return verifyNever(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments 0, 0, {@code threadMatcher}
     * @since 2.0
     */
    public C verifyNever(ThreadMatcher threadMatcher) {
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
    public C verifyAtMostOnce() {
        return verifyAtMostOnce(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments 0, 1, {@code threadMatcher}
     * @since 2.0
     */
    public C verifyAtMostOnce(ThreadMatcher threadMatcher) {
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
    public C verifyAtMost(int allowedStatements) {
        return verifyAtMost(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments 0, {@code allowedStatements}, {@code threadMatcher}
     * @since 2.0
     */
    public C verifyAtMost(int allowedStatements, ThreadMatcher threadMatcher) {
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
    public C verify(int allowedStatements) {
        return verify(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threadMatcher}
     * @since 2.0
     */
    public C verify(int allowedStatements, ThreadMatcher threadMatcher) {
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
    public C verifyAtLeast(int allowedStatements) {
        return verifyAtLeast(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, ThreadMatcher)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threadMatcher}
     * @since 2.0
     */
    public C verifyAtLeast(int allowedStatements, ThreadMatcher threadMatcher) {
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
     * @since 2.0
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, ThreadMatcher threadMatcher) {
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher));
        return self();
    }

    /**
     * @since 2.0
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements) {
        return verifyBetween(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * @since 2.0
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, ThreadMatcher threadMatcher) {
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher).validate();
        return self();
    }

    // end

    /**
     * @since 2.0
     */
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
    /**
     * @since 2.0
     */
    public void close() {
        verify();
    }

    /**
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

    /**
     * @since 2.0
     */
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

    /**
     * @since 2.0
     */
    private interface Expectation {
        /**
         * @since 2.0
         */
        void validate() throws AssertionError;
    }

    /**
     * @since 2.0
     */
    private class ThreadMatcherExpectation implements Expectation {

        private final int minimumQueries;
        private final int maximumQueries;
        private final ThreadMatcher threadMatcher;

        /**
         * @since 2.0
         */
        public ThreadMatcherExpectation(int minimumQueries, int maximumQueries, ThreadMatcher threadMatcher) {
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
            this.threadMatcher = threadMatcher;
        }

        /**
         * @since 2.0
         * @todo should throw some more specific exception
         */
        @Override
        public void validate() throws AssertionError {

            if (threadMatcher instanceof Sniffer.AnyThread) {
                int numQueries = Sniffer.executedStatements() - initialQueries;
                if (numQueries > maximumQueries || numQueries < minimumQueries)
                    throw new AssertionError(String.format(
                            "Disallowed number of executed statements; expected between %d and %d; observed %d",
                            minimumQueries, maximumQueries, numQueries
                    ));
            } else if (threadMatcher instanceof Sniffer.CurrentThread) {
                int numQueries = Sniffer.ThreadLocalSniffer.executedStatements() - initialThreadLocalQueries;
                if (numQueries > maximumQueries || numQueries < minimumQueries)
                    throw new AssertionError(String.format(
                            "Disallowed number of executed statements; expected between %d and %d; observed %d",
                            minimumQueries, maximumQueries, numQueries
                    ));
            } else if (threadMatcher instanceof Sniffer.OtherThreads) {
                int numQueries = Sniffer.executedStatements() - Sniffer.ThreadLocalSniffer.executedStatements()
                        - initialQueries + initialThreadLocalQueries;
                if (numQueries > maximumQueries || numQueries < minimumQueries)
                    throw new AssertionError(String.format(
                            "Disallowed number of executed statements; expected between %d and %d; observed %d",
                            minimumQueries, maximumQueries, numQueries
                    ));
            } else {
                throw new AssertionError(String.format("Unknown thread matcher %s", threadMatcher.getClass().getName()));
            }

        }

    }

    /**
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    private C self() {
        return (C) this;
    }

}
