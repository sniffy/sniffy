package com.github.bedrin.jdbc.sniffer;

import java.util.Collection;
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

    private final AtomicInteger counter = new AtomicInteger();

    private static final Sniffer INSTANCE = new Sniffer();

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

    static Collection<Sniffer> getThreadLocalSniffers() {
        return threadLocalSniffers;
    }

    static void executeStatement() {
        INSTANCE.executeStatementImpl();
        ThreadLocalSniffer.executeStatement();
    }

    @Deprecated
    private volatile int checkpoint = 0;
    @Deprecated
    private volatile ExpectedQueries expectedQueries = new ExpectedQueries(0, 0);

    int executedStatementsImpl(boolean sinceLastReset) {
        int counter = this.counter.get();
        return sinceLastReset ? counter - checkpoint : counter;
    }

    @Deprecated
    ExpectedQueries resetImpl() {
        checkpoint = executedStatementsImpl(false);
        return expectedQueries = new ExpectedQueries();
    }

    /**
     * @return the number of executed queries since the last call of {@link #reset() reset} method or to any of verify
     * methods family like {@link #verifyNotMore() verifyNotMore}, {@link #verifyNotMoreThanOne() verifyNotMoreThanOne}
     * or {@link #verifyNotMoreThan(int) verifyNotMoreThan}
     * @since 1.0
     */
    public static int executedStatements() {
        return executedStatements(true);
    }

    /**
     * @since 2.0
     */
    public static int executedStatements(boolean sinceLastReset) {
        return INSTANCE.executedStatementsImpl(sinceLastReset);
    }

    /**
     * Resets the queries counter to 0
     * @since 1.0
     */
    @Deprecated
    public static ExpectedQueries reset() {
        return INSTANCE.resetImpl();
    }

    /**
     * Verifies that no queries has been executed since the last call of {@link #reset() reset} method or to any of verify
     * methods family
     * @throws AssertionError if actual number of executed statements exceeded 0
     * @since 1.0
     */
    @Deprecated
    public static ExpectedQueries verifyNotMore() {
        return verifyNotMoreThan(0);
    }

    /**
     * Verifies that at most 1 query has been executed since the last call of {@link #reset() reset} method or to any of verify
     * methods family
     * @throws AssertionError if actual number of executed statements exceeded 1
     * @since 1.0
     */
    @Deprecated
    public static ExpectedQueries verifyNotMoreThanOne() {
        return verifyNotMoreThan(1);
    }

    /**
     * Verifies that at most {@code allowedStatements} query has been executed since the last call of
     * {@link #reset() reset} method or to any of verify methods family
     * @param allowedStatements maximum number of statements which could have been executed previously since
     *                          last {@link #reset() resetC} call
     * @throws AssertionError if actual number of executed statements exceeded {@code allowedStatements}
     * @since 1.0
     */
    @Deprecated
    public static ExpectedQueries verifyNotMoreThan(int allowedStatements) throws AssertionError {
        return verifyRange(0, allowedStatements);
    }

    /**
     * Verifies that exactly {@code allowedStatements} queries has been executed since the last call
     * of {@link #reset() reset} method or to any of verify methods family
     * @param allowedStatements number of statements which could have been executed previously since
     *                          last {@link #reset() reset} call
     * @throws AssertionError if illegal number of queries were performed
     * @since 1.3
     */
    @Deprecated
    public static ExpectedQueries verifyExact(int allowedStatements) throws AssertionError {
        return verifyRange(allowedStatements, allowedStatements);
    }

    /**
     * Verifies that at least {@code allowedStatements} queries has been executed since the last call
     * of {@link #reset() reset} method or to any of verify methods family
     * @param allowedStatements minimum number of statements which could have been executed previously since
     *                          last {@link #reset() reset} call
     * @throws AssertionError if illegal number of queries were performed
     * @since 1.3
     */
    @Deprecated
    public static ExpectedQueries verifyNotLessThan(int allowedStatements) throws AssertionError {
        return verifyRange(allowedStatements, Integer.MAX_VALUE);
    }

    /**
     * Verifies that at least {@code minAllowedStatements} queries and at most {@code maxAllowedStatements} has been
     * executed since the last call of {@link #reset() reset} method or to any of verify methods family
     * @param minAllowedStatements minimum number of statements which could have been executed previously since
     *                             last {@link #reset() reset} call
     * @param maxAllowedStatements maximum number of statements which could have been executed previously since
     *                             last {@link #reset() reset} call
     * @throws AssertionError if illegal number of queries were performed
     * @since 1.3
     */
    @Deprecated
    public static ExpectedQueries verifyRange(int minAllowedStatements, int maxAllowedStatements) throws AssertionError {
        INSTANCE.expectedQueries.verifyRange(minAllowedStatements, maxAllowedStatements);
        return reset();
    }

    /**
     * Executable interface is similar to {@link java.lang.Runnable} but it allows throwing {@link java.lang.Exception}
     * from it's {@link #execute()} method
     */
    public static interface Executable {

        /**
         * When {@link com.github.bedrin.jdbc.sniffer.Sniffer#execute(com.github.bedrin.jdbc.sniffer.Sniffer.Executable)}
         * method is called, it will execute the Executable.execute() method, record the SQL queries and return the
         * {@link com.github.bedrin.jdbc.sniffer.RecordedQueries} object with stats
         * @throws Exception code under test can throw any exception
         */
        void execute() throws Exception;

    }

    /**
     * Execute the {@link com.github.bedrin.jdbc.sniffer.Sniffer.Executable#execute()} method, record the SQL queries
     * and return the {@link com.github.bedrin.jdbc.sniffer.RecordedQueries} object with stats
     * @param executable code to test
     * @return statistics on executed queries
     * @throws RuntimeException if underlying code under test throws an Exception
     */
    public static RecordedQueries execute(Executable executable) {
        return new ExpectedQueries().execute(executable);
    }

    /**
     * Execute the {@link Runnable#run()} method, record the SQL queries
     * and return the {@link com.github.bedrin.jdbc.sniffer.RecordedQueries} object with stats
     * @param runnable code to test
     * @return statistics on executed queries
     */
    public static RecordedQueries run(Runnable runnable) {
        return new ExpectedQueries().run(runnable);
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
        return new ExpectedQueries().call(callable);
    }

}
