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
    public static <T extends ExpectedQueries<T>> ExpectedQueries<T> expectedQueries() {
        return new ExpectedQueries<T>();
    }

    // noMore methods

    /**
     * @since 2.0
     */
    public static ExpectedQueries expectNoMore() {
        return expectNoMore(DEFAULT_THREAD_MATCHER);
    }

    /**
     * @since 2.0
     */
    public static ExpectedQueries expectNoMore(ThreadMatcher threadMatcher) {
        return expectedQueries().expectNoMore(threadMatcher);
    }

    // notMoreThanOne methods

    /**
     * @since 2.0
     */
    public static ExpectedQueries expectNotMoreThanOne() {
        return expectNotMoreThanOne(DEFAULT_THREAD_MATCHER);
    }

    /**
     * @since 2.0
     */
    public static ExpectedQueries expectNotMoreThanOne(ThreadMatcher threadMatcher) {
        return expectedQueries().expectNotMoreThanOne(threadMatcher);
    }

    // notMoreThan methods

    /**
     * @since 2.0
     */
    public static ExpectedQueries expectNotMoreThan(int allowedStatements) {
        return expectNotMoreThan(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * @since 2.0
     */
    public static ExpectedQueries expectNotMoreThan(int allowedStatements, ThreadMatcher threadMatcher) {
        return expectedQueries().expectNotMoreThan(allowedStatements, threadMatcher);
    }

    /**
     * Executable interface is similar to {@link java.lang.Runnable} but it allows throwing {@link java.lang.Exception}
     * from it's {@link #execute()} method
     */
    public interface Executable {

        /**
         * When {@link com.github.bedrin.jdbc.sniffer.Sniffer#execute(com.github.bedrin.jdbc.sniffer.Sniffer.Executable)}
         * method is called, it will execute the Executable.execute() method, record the SQL queries and return the
         * {@link com.github.bedrin.jdbc.sniffer.ExpectedQueries} object with stats
         * @throws Exception code under test can throw any exception
         */
        void execute() throws Exception;

    }

    /**
     * Execute the {@link com.github.bedrin.jdbc.sniffer.Sniffer.Executable#execute()} method, record the SQL queries
     * and return the {@link com.github.bedrin.jdbc.sniffer.ExpectedQueries} object with stats
     * @param executable code to test
     * @return statistics on executed queries
     * @throws RuntimeException if underlying code under test throws an Exception
     */
    public static ExpectedQueries execute(Executable executable) {
        return expectedQueries().execute(executable);
    }

    /**
     * Execute the {@link Runnable#run()} method, record the SQL queries
     * and return the {@link com.github.bedrin.jdbc.sniffer.ExpectedQueries} object with stats
     * @param runnable code to test
     * @return statistics on executed queries
     */
    public static ExpectedQueries run(Runnable runnable) {
        return expectedQueries().run(runnable);
    }

    /**
     * Execute the {@link Callable#call()} method, record the SQL queries
     * and return the {@link com.github.bedrin.jdbc.sniffer.RecordedQueriesWithValue} object with stats
     * @param callable code to test
     * @param <V> type of return value
     * @return statistics on executed queries
     * @throws Exception if underlying code under test throws an Exception
     */
    public static <V> RecordedQueriesWithValue<V> call(Callable<V> callable) throws Exception {
        return expectedQueries().call(callable);
    }

    public static final AnyThread ANY_THREAD = new AnyThread();
    public static final CurrentThread CURRENT_THREAD = new CurrentThread();
    public static final OtherThreads OTHER_THREADS = new OtherThreads();

    protected final static ThreadMatcher DEFAULT_THREAD_MATCHER = ANY_THREAD;

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