package com.github.bedrin.jdbc.sniffer;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sniffer holds the number of executed queries and provides static methods for accessing them
 * @since 1.0
 */
public class Sniffer {

    private static final Sniffer INSTANCE = new Sniffer();

    private final AtomicInteger counter = new AtomicInteger();

    int executeStatementImpl() {
        return counter.incrementAndGet();
    }

    static void executeStatement() {
        INSTANCE.executeStatementImpl();
        ThreadLocalSniffer.executeStatement();
    }

    int executedStatementsImpl() {
        return this.counter.get();
    }

    /**
     * @since 1.0
     */
    public static int executedStatements() {
        return INSTANCE.executedStatementsImpl();
    }

    /**
     * @since 2.0
     */
    public static <T extends Spy<T>> Spy<T> spy() {
        return new Spy<T>();
    }

    // noMore methods

    /**
     * @since 2.0
     */
    public static Spy expectNoMore() {
        return expectNoMore(DEFAULT_THREAD_MATCHER);
    }

    /**
     * @since 2.0
     */
    public static Spy expectNoMore(ThreadMatcher threadMatcher) {
        return spy().expectNever(threadMatcher);
    }

    // notMoreThanOne methods

    /**
     * @since 2.0
     */
    public static Spy expectNotMoreThanOne() {
        return expectNotMoreThanOne(DEFAULT_THREAD_MATCHER);
    }

    /**
     * @since 2.0
     */
    public static Spy expectNotMoreThanOne(ThreadMatcher threadMatcher) {
        return spy().expectAtMostOnce(threadMatcher);
    }

    // notMoreThan methods

    /**
     * @since 2.0
     */
    public static Spy expectNotMoreThan(int allowedStatements) {
        return expectNotMoreThan(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * @since 2.0
     */
    public static Spy expectNotMoreThan(int allowedStatements, ThreadMatcher threadMatcher) {
        return spy().expectAtMost(allowedStatements, threadMatcher);
    }

    /**
     * Executable interface is similar to {@link java.lang.Runnable} but it allows throwing {@link java.lang.Exception}
     * from it's {@link #execute()} method
     */
    public interface Executable {

        /**
         * When {@link com.github.bedrin.jdbc.sniffer.Sniffer#execute(com.github.bedrin.jdbc.sniffer.Sniffer.Executable)}
         * method is called, it will execute the Executable.execute() method, record the SQL queries and return the
         * {@link Spy} object with stats
         * @throws Exception code under test can throw any exception
         */
        void execute() throws Exception;

    }

    /**
     * Execute the {@link com.github.bedrin.jdbc.sniffer.Sniffer.Executable#execute()} method, record the SQL queries
     * and return the {@link Spy} object with stats
     * @param executable code to test
     * @return statistics on executed queries
     * @throws RuntimeException if underlying code under test throws an Exception
     */
    public static Spy execute(Executable executable) {
        return spy().execute(executable);
    }

    /**
     * Execute the {@link Runnable#run()} method, record the SQL queries
     * and return the {@link Spy} object with stats
     * @param runnable code to test
     * @return statistics on executed queries
     */
    public static Spy run(Runnable runnable) {
        return spy().run(runnable);
    }

    /**
     * Execute the {@link Callable#call()} method, record the SQL queries
     * and return the {@link SpyWithValue} object with stats
     * @param callable code to test
     * @param <V> type of return value
     * @return statistics on executed queries
     * @throws Exception if underlying code under test throws an Exception
     */
    public static <V> SpyWithValue<V> call(Callable<V> callable) throws Exception {
        return spy().call(callable);
    }

    public static final AnyThread ANY_THREAD = new AnyThread();
    public static final CurrentThread CURRENT_THREAD = new CurrentThread();
    public static final OtherThreads OTHER_THREADS = new OtherThreads();

    protected final static ThreadMatcher DEFAULT_THREAD_MATCHER = CURRENT_THREAD;

    protected abstract static class ThreadMatcher {

    }

    static class AnyThread extends ThreadMatcher {

    }

    static class CurrentThread extends ThreadMatcher {

    }

    static class OtherThreads extends ThreadMatcher {

    }

    static class ThreadLocalSniffer extends ThreadLocal<Sniffer> {

        private final static ThreadLocalSniffer INSTANCE = new ThreadLocalSniffer();

        @Override
        protected Sniffer initialValue() {
            return new Sniffer();
        }

        static Sniffer getSniffer() {
            return INSTANCE.get();
        }

        static void executeStatement() {
            getSniffer().executeStatementImpl();
        }

        static int executedStatements() {
            return getSniffer().executedStatementsImpl();
        }

    }

}