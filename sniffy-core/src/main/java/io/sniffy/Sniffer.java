package io.sniffy;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @see Sniffy
 */
@Deprecated
public final class Sniffer extends Sniffy {

    @Deprecated
    public final static AtomicInteger executedStatementsGlobalCounter = new AtomicInteger();

    @Deprecated
    private Sniffer() {

    }

    /**
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    @Deprecated
    public static <T extends Spy<T>> Spy<? extends Spy<T>> spy() {
        return Sniffy.<T>spy();
    }

    /**
     * Executable interface is similar to {@link java.lang.Runnable} but it allows throwing {@link java.lang.Exception}
     * from it's {@link #execute()} method
     * @since 2.0
     */
    @Deprecated
    public interface Executable extends io.sniffy.Executable {}

    /**
     * Execute the {@link Sniffer.Executable#execute()} method, record the SQL queries
     * and return the {@link Spy} object with stats
     * @param executable code to test
     * @return statistics on executed queries
     * @throws RuntimeException if underlying code under test throws an Exception
     * @since 2.0
     */
    @Deprecated
    public static Spy execute(Executable executable) {
        return Sniffy.execute(executable);
    }

    /**
     * Execute the {@link Runnable#run()} method, record the SQL queries
     * and return the {@link Spy} object with stats
     * @param runnable code to test
     * @return statistics on executed queries
     * @since 2.0
     */
    @Deprecated
    public static Spy run(Runnable runnable) {
        return Sniffy.run(runnable);
    }

    /**
     * Execute the {@link Callable#call()} method, record the SQL queries
     * and return the {@link Spy.SpyWithValue} object with stats
     * @param callable code to test
     * @param <V> type of return value
     * @return statistics on executed queries
     * @throws Exception if underlying code under test throws an Exception
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static <V> Spy.SpyWithValue<V> call(Callable<V> callable) throws Exception {
        return Sniffy.call(callable);
    }

    /**
     * @return number of SQL statements executed by current thread since some fixed moment of time
     * @since 1.0
     */
    @Deprecated
    public static int executedStatements() {
        return executedStatementsGlobalCounter.intValue();
    }

    // never methods

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever()
     * @since 2.0
     */
    @Deprecated
    public static Spy expectNever() {
        return spy().expectNever();
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever(Threads)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectNever(Threads threadMatcher) {
        return spy().expectNever(threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever(Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectNever(Query query) {
        return spy().expectNever(query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever(Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectNever(Threads threadMatcher, Query query) {
        return spy().expectNever(threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever(Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectNever(Query query, Threads threadMatcher) {
        return spy().expectNever(query, threadMatcher);
    }

    // atMostOnce methods

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce()
     * @since 2.0
     */
    @Deprecated
    public static Spy expectAtMostOnce() {
        return spy().expectAtMostOnce();
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce(Threads)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectAtMostOnce(Threads threadMatcher) {
        return spy().expectAtMostOnce(threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce(Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectAtMostOnce(Query query) {
        return spy().expectAtMostOnce(query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce(Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectAtMostOnce(Threads threadMatcher, Query query) {
        return spy().expectAtMostOnce(threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce(Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectAtMostOnce(Query query, Threads threadMatcher) {
        return spy().expectAtMostOnce(query, threadMatcher);
    }

    // notMoreThan methods

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectAtMost(int allowedStatements) {
        return spy().expectAtMost(allowedStatements);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int, Threads)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectAtMost(int allowedStatements, Threads threadMatcher) {
        return spy().expectAtMost(allowedStatements, threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectAtMost(int allowedStatements, Query query) {
        return spy().expectAtMost(allowedStatements, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int, Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectAtMost(int allowedStatements, Threads threadMatcher, Query query) {
        return spy().expectAtMost(allowedStatements, threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int, Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectAtMost(int allowedStatements, Query query, Threads threadMatcher) {
        return spy().expectAtMost(allowedStatements, query, threadMatcher);
    }

    // exact methods

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int)
     * @since 2.0
     */
    @Deprecated
    public static Spy expect(int allowedStatements) {
        return spy().expect(allowedStatements);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int, Threads)
     * @since 2.0
     */
    @Deprecated
    public static Spy expect(int allowedStatements, Threads threadMatcher) {
        return spy().expect(allowedStatements, threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expect(int allowedStatements, Query query) {
        return spy().expect(allowedStatements, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int, Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expect(int allowedStatements, Threads threadMatcher, Query query) {
        return spy().expect(allowedStatements, threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int, Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expect(int allowedStatements, Query query, Threads threadMatcher) {
        return spy().expect(allowedStatements, query, threadMatcher);
    }

    // atLeast methods

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectAtLeast(int allowedStatements) {
        return spy().expectAtLeast(allowedStatements);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int, Threads)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectAtLeast(int allowedStatements, Threads threadMatcher) {
        return spy().expectAtLeast(allowedStatements, threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectAtLeast(int allowedStatements, Query query) {
        return spy().expectAtLeast(allowedStatements, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int, Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectAtLeast(int allowedStatements, Threads threadMatcher, Query query) {
        return spy().expectAtLeast(allowedStatements, threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int, Threads, Query)
     * @since 2.2
     */
    @Deprecated
    public static Spy expectAtLeast(int allowedStatements, Query query, Threads threadMatcher) {
        return spy().expectAtLeast(allowedStatements, query, threadMatcher);
    }

    // between methods methods

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy
     * @see Spy#expectBetween(int, int)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectBetween(int, int, Threads)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectBetween(int, int, Query)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectBetween(int, int, Threads, Query)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query query) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectBetween(int, int, Threads, Query)
     * @since 2.0
     */
    @Deprecated
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query, Threads threadMatcher) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, query, threadMatcher);
    }

}