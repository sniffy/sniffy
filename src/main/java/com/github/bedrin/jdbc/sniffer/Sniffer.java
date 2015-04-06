package com.github.bedrin.jdbc.sniffer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
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

    final static Set<Sniffer> threadLocalSniffers = Collections.newSetFromMap(
            new ConcurrentHashMap<Sniffer, Boolean>()
    );

    static Sniffer registerThreadLocalSniffer(Sniffer tlSniffer) {
        threadLocalSniffers.add(tlSniffer);
        return tlSniffer;
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
    public static ExpectedQueries expectedQueries() {
        return new ExpectedQueries();
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
     * @param <T> type of return value
     * @return statistics on executed queries
     * @throws Exception if underlying code under test throws an Exception
     */
    public static <T> RecordedQueriesWithValue<T> call(Callable<T> callable) throws Exception {
        return expectedQueries().call(callable);
    }

    public static final AnyThread ANY_THREAD = new AnyThread();
    public static final CurrentThread CURRENT_THREAD = new CurrentThread();
    public static final OtherThreads OTHER_THREADS = new OtherThreads();

    protected final static ThreadMatcher DEFAULT_THREAD_MATCHER = ANY_THREAD;

    protected abstract static class ThreadMatcher {

    }

    public static class AnyThread extends ThreadMatcher {

    }

    public static class CurrentThread extends ThreadMatcher {

    }

    public static class OtherThreads extends ThreadMatcher {

    }

}
