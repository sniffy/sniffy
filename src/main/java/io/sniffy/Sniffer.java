package io.sniffy;

import io.sniffy.log.QueryLogger;
import io.sniffy.sql.StatementMetaData;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Sniffer is an entry point for using Sniffy library
 * See the
 * <a href="{@docRoot}/io/sniffy/package-summary.html#package_description">
 *    package overview
 * </a> for more information.
 * @since 1.0
 */
public final class Sniffer {

    private Sniffer() {

    }

    // Registered listeners (i.e. spies)

    private static final List<WeakReference<Spy>> registeredSpies = new LinkedList<WeakReference<Spy>>();

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

    private static synchronized void notifyListeners(StatementMetaData statementMetaData) {
        Iterator<WeakReference<Spy>> iterator = registeredSpies.iterator();
        while (iterator.hasNext()) {
            WeakReference<Spy> spyReference = iterator.next();
            Spy spy = spyReference.get();
            if (null == spy) {
                iterator.remove();
            } else {
                spy.addExecutedStatement(statementMetaData);
            }
        }
    }

    // query counters

    protected static final Counter COUNTER = new Counter();

    protected static final ThreadLocal<Counter> THREAD_LOCAL_COUNTER = new ThreadLocal<Counter>() {

        @Override
        protected Counter initialValue() {
            return new Counter();
        }

    };

    protected static void executeStatement(String sql, long elapsedTime, String stackTrace) {
        // log query
        QueryLogger.logQuery(sql, elapsedTime);

        // increment counters
        StatementMetaData statementMetaData = StatementMetaData.parse(sql, elapsedTime, stackTrace);
        COUNTER.executeStatement(statementMetaData.query);
        THREAD_LOCAL_COUNTER.get().executeStatement(statementMetaData.query);

        // notify listeners
        notifyListeners(statementMetaData);
    }

    /**
     * @return number of SQL statements executed by current thread since some fixed moment of time
     * @since 1.0
     */
    public static int executedStatements() {
        return COUNTER.executedStatements(Query.ANY);
    }

    /**
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    public static <T extends Spy<T>> Spy<? extends Spy<T>> spy() {
        return new Spy<T>();
    }

    // never methods

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever()
     * @since 2.0
     */
    public static Spy expectNever() {
        return spy().expectNever();
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever(Threads)
     * @since 2.0
     */
    public static Spy expectNever(Threads threadMatcher) {
        return spy().expectNever(threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever(Query)
     * @since 2.2
     */
    public static Spy expectNever(Query query) {
        return spy().expectNever(query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever(Threads, Query)
     * @since 2.2
     */
    public static Spy expectNever(Threads threadMatcher, Query query) {
        return spy().expectNever(threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever(Threads, Query)
     * @since 2.2
     */
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
    public static Spy expectAtMostOnce() {
        return spy().expectAtMostOnce();
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce(Threads)
     * @since 2.0
     */
    public static Spy expectAtMostOnce(Threads threadMatcher) {
        return spy().expectAtMostOnce(threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce(Query)
     * @since 2.2
     */
    public static Spy expectAtMostOnce(Query query) {
        return spy().expectAtMostOnce(query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce(Threads, Query)
     * @since 2.2
     */
    public static Spy expectAtMostOnce(Threads threadMatcher, Query query) {
        return spy().expectAtMostOnce(threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce(Threads, Query)
     * @since 2.2
     */
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
    public static Spy expectAtMost(int allowedStatements) {
        return spy().expectAtMost(allowedStatements);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int, Threads)
     * @since 2.0
     */
    public static Spy expectAtMost(int allowedStatements, Threads threadMatcher) {
        return spy().expectAtMost(allowedStatements, threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int, Query)
     * @since 2.2
     */
    public static Spy expectAtMost(int allowedStatements, Query query) {
        return spy().expectAtMost(allowedStatements, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int, Threads, Query)
     * @since 2.2
     */
    public static Spy expectAtMost(int allowedStatements, Threads threadMatcher, Query query) {
        return spy().expectAtMost(allowedStatements, threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int, Threads, Query)
     * @since 2.2
     */
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
    public static Spy expect(int allowedStatements) {
        return spy().expect(allowedStatements);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int, Threads)
     * @since 2.0
     */
    public static Spy expect(int allowedStatements, Threads threadMatcher) {
        return spy().expect(allowedStatements, threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int, Query)
     * @since 2.2
     */
    public static Spy expect(int allowedStatements, Query query) {
        return spy().expect(allowedStatements, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int, Threads, Query)
     * @since 2.2
     */
    public static Spy expect(int allowedStatements, Threads threadMatcher, Query query) {
        return spy().expect(allowedStatements, threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int, Threads, Query)
     * @since 2.2
     */
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
    public static Spy expectAtLeast(int allowedStatements) {
        return spy().expectAtLeast(allowedStatements);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int, Threads)
     * @since 2.0
     */
    public static Spy expectAtLeast(int allowedStatements, Threads threadMatcher) {
        return spy().expectAtLeast(allowedStatements, threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int, Query)
     * @since 2.2
     */
    public static Spy expectAtLeast(int allowedStatements, Query query) {
        return spy().expectAtLeast(allowedStatements, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int, Threads, Query)
     * @since 2.2
     */
    public static Spy expectAtLeast(int allowedStatements, Threads threadMatcher, Query query) {
        return spy().expectAtLeast(allowedStatements, threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int, Threads, Query)
     * @since 2.2
     */
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
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectBetween(int, int, Threads)
     * @since 2.0
     */
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, threadMatcher);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectBetween(int, int, Query)
     * @since 2.0
     */
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectBetween(int, int, Threads, Query)
     * @since 2.0
     */
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query query) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, threadMatcher, query);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectBetween(int, int, Threads, Query)
     * @since 2.0
     */
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query, Threads threadMatcher) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, query, threadMatcher);
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
            if (-1 != expectation.value()) {
                spy.expect(expectation.value(), expectation.threads(), expectation.query());
            }
            if (-1 != expectation.atLeast() && -1 != expectation.atMost()) {
                spy.expectBetween(expectation.atLeast(), expectation.atMost(),
                        expectation.threads(), expectation.query());
            } else if (-1 != expectation.atLeast()) {
                spy.expectAtLeast(expectation.atLeast(),
                        expectation.threads(), expectation.query());
            } else if (-1 != expectation.atMost()) {
                spy.expectAtMost(expectation.atMost(),
                        expectation.threads(), expectation.query());
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

    protected final static Threads DEFAULT_THREAD_MATCHER = Threads.CURRENT;

}