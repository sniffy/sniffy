package io.sniffy;

import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlQueries;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.Range;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static io.sniffy.util.StackTraceExtractor.getTraceForProxiedMethod;
import static io.sniffy.util.StackTraceExtractor.printStackTrace;

/**
 * Sniffer is an entry point for using Sniffy library
 * See the
 * <a href="{@docRoot}/io/sniffy/package-summary.html#package_description">
 *    package overview
 * </a> for more information.
 * @since 1.0
 */
public final class Sniffer {

    @Deprecated
    private final static AtomicInteger executedStatementsGlobalCounter = new AtomicInteger();

    private static final List<WeakReference<Spy>> registeredSpies = new LinkedList<WeakReference<Spy>>();

    private static ThreadLocal<SocketStats> socketStatsAccumulator = new ThreadLocal<SocketStats>();

    private Sniffer() {

    }

    static {
        initialize();
    }

    public static void initialize() {
        try {
            SnifferSocketImplFactory.install();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static synchronized WeakReference<Spy> registerSpy(Spy spy) {
        WeakReference<Spy> spyReference = new WeakReference<Spy>(spy);
        registeredSpies.add(spyReference);
        return spyReference;
    }

    protected static synchronized void removeSpyReference(WeakReference<Spy> spyReference) {
        registeredSpies.remove(spyReference);
    }

    protected static List<WeakReference<Spy>> registeredSpies() {
        return Collections.unmodifiableList(registeredSpies);
    }

    private static synchronized void notifyListeners(StatementMetaData statementMetaData, long elapsedTime, int bytesDown, int bytesUp, int rowsUpdated) {
        Iterator<WeakReference<Spy>> iterator = registeredSpies.iterator();
        while (iterator.hasNext()) {
            WeakReference<Spy> spyReference = iterator.next();
            Spy spy = spyReference.get();
            if (null == spy) {
                iterator.remove();
            } else {
                spy.addExecutedStatement(statementMetaData, elapsedTime, bytesDown, bytesUp, rowsUpdated);
            }
        }
    }

    private static synchronized void notifyListeners(StatementMetaData statementMetaData) {
        Iterator<WeakReference<Spy>> iterator = registeredSpies.iterator();
        while (iterator.hasNext()) {
            WeakReference<Spy> spyReference = iterator.next();
            Spy spy = spyReference.get();
            if (null == spy) {
                iterator.remove();
            } else {
                spy.addReturnedRow(statementMetaData);
            }
        }
    }

    private static synchronized void notifyListeners(SocketMetaData socketMetaData, long elapsedTime, int bytesDown, int bytesUp) {
        Iterator<WeakReference<Spy>> iterator = registeredSpies.iterator();
        while (iterator.hasNext()) {
            WeakReference<Spy> spyReference = iterator.next();
            Spy spy = spyReference.get();
            if (null == spy) {
                iterator.remove();
            } else {
                spy.addSocketOperation(socketMetaData, elapsedTime, bytesDown, bytesUp);
            }
        }
    }

    public static void logSocket(String stackTrace, int connectionId, InetSocketAddress address, long elapsedTime, int bytesDown, int bytesUp) {

        // do not track JDBC socket operations
        SocketStats socketStats = socketStatsAccumulator.get();
        if (null != socketStats) {
            socketStats.accumulate(elapsedTime, bytesDown, bytesUp);
        } else {
            // increment counters
            SocketMetaData socketMetaData = new SocketMetaData(address, connectionId, stackTrace, Thread.currentThread().getId());

            // notify listeners
            notifyListeners(socketMetaData, elapsedTime, bytesDown, bytesUp);
        }
    }

    public static void enterJdbcMethod() {
        socketStatsAccumulator.set(new SocketStats(0, 0, 0));
    }

    public static void exitJdbcMethod(Method method, long elapsedTime) {
        // get accumulated socket stats
        SocketStats socketStats = socketStatsAccumulator.get();

        if (null != socketStats) {

            if (socketStats.bytesDown.longValue() > 0 || socketStats.bytesUp.longValue() > 0) {
                String stackTrace = null;
                try {
                    stackTrace = printStackTrace(getTraceForProxiedMethod(method));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                StatementMetaData statementMetaData = new StatementMetaData(
                        method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()",
                        Query.SYSTEM,
                        stackTrace,
                        Thread.currentThread().getId()
                );
                notifyListeners(
                        statementMetaData,
                        elapsedTime,
                        socketStats.bytesDown.intValue(),
                        socketStats.bytesUp.intValue(),
                        0
                );
            }

            socketStatsAccumulator.remove();
        }

    }

    public static void readDatabaseRow(Method method, long elapsedTime, StatementMetaData statementMetaData) {
        exitJdbcMethod(method, elapsedTime);

        notifyListeners(statementMetaData);
    }

    public static StatementMetaData executeStatement(String sql, long elapsedTime, String stackTrace) {
        return executeStatement(sql, elapsedTime, stackTrace, 0);
    }

    public static StatementMetaData executeStatement(String sql, long elapsedTime, String stackTrace, int rowsUpdated) {
        // increment global counter
        executedStatementsGlobalCounter.incrementAndGet();

        // get accumulated socket stats
        SocketStats socketStats = socketStatsAccumulator.get();

        // notify listeners
        StatementMetaData statementMetaData = new StatementMetaData(sql, StatementMetaData.guessQueryType(sql), stackTrace, Thread.currentThread().getId());
        notifyListeners(
                statementMetaData,
                elapsedTime,
                null == socketStats ? 0 : socketStats.bytesDown.intValue(),
                null == socketStats ? 0 : socketStats.bytesUp.intValue(),
                rowsUpdated
        );

        socketStatsAccumulator.remove();

        return statementMetaData;
    }

    /**
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    public static <T extends Spy<T>> Spy<? extends Spy<T>> spy() {
        return new Spy<T>(false);
    }

    /**
     * @return a new {@link Spy} instance
     * @since 3.1
     */
    public static <T extends Spy<T>> Spy<? extends Spy<T>> spyCurrentThread() {
        return new Spy<T>(true);
    }

    public static Spy expect(Spy.Expectation expectation) {
        return spy().expect(expectation);
    }

    /**
     * @param expectationList a list of {@link Expectation} annotations
     * @return a new {@link Spy} instance with given expectations
     * @see #spy()
     * @since 2.1
     */
    public static Spy expect(List<Expectation> expectationList) {
        Spy spy = Sniffer.spy();

        for (Expectation expectation : expectationList) {

            Range queriesRange = Range.parse(expectation);
            Range rowsRange = Range.parse(expectation.rows());

            if (-1 != queriesRange.min || -1 != queriesRange.max || -1 != rowsRange.min || -1 != rowsRange.max) {
                spy.expect(SqlQueries.
                        queriesBetween(-1 == queriesRange.min ? 0 : queriesRange.min, -1 == queriesRange.max ? Integer.MAX_VALUE : queriesRange.max).
                        rowsBetween(-1 == rowsRange.min ? 0 : rowsRange.min, -1 == rowsRange.max ? Integer.MAX_VALUE : rowsRange.max).
                        threads(expectation.threads()).
                        type(expectation.query())
                );
            }

        }

        return spy;
    }

    /**
     * Executable interface is similar to {@link java.lang.Runnable} but it allows throwing {@link java.lang.Exception}
     * from it's {@link #execute()} method
     */
    public interface Executable {

        /**
         * When {@link Sniffer#execute(Sniffer.Executable)}
         * method is called, it will execute the Executable.execute() method, record the SQL queries and return the
         * {@link Spy} object with stats
         * @throws Exception code under test can throw any exception
         */
        void execute() throws Throwable;

    }

    /**
     * Execute the {@link Sniffer.Executable#execute()} method, record the SQL queries
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
    @SuppressWarnings("unchecked")
    public static <V> SpyWithValue<V> call(Callable<V> callable) throws Exception {
        return spy().call(callable);
    }



    // DEPRECATED API




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