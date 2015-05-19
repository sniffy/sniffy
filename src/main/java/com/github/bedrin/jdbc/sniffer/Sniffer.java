package com.github.bedrin.jdbc.sniffer;

import com.github.bedrin.jdbc.sniffer.junit.Expectation;
import com.github.bedrin.jdbc.sniffer.log.QueryLogger;
import com.github.bedrin.jdbc.sniffer.sql.Query;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sniffer is an entry point for using JDBC Sniffer library
 * See the
 * <a href="{@docRoot}/com/github/bedrin/jdbc/sniffer/package-summary.html#package_description">
 *    package overview
 * </a> for more information.
 * @since 1.0
 */
public class Sniffer {

    private static final Sniffer INSTANCE = new Sniffer();

    private final AtomicInteger counterSelect = new AtomicInteger();
    private final AtomicInteger counterInsert = new AtomicInteger();
    private final AtomicInteger counterUpdate = new AtomicInteger();
    private final AtomicInteger counterDelete = new AtomicInteger();
    private final AtomicInteger counterOther = new AtomicInteger();

    private final List<WeakReference<Spy>> registeredSpies = new LinkedList<WeakReference<Spy>>();

    synchronized WeakReference<Spy> registerSpyImpl(Spy spy) {
        WeakReference<Spy> spyReference = new WeakReference<Spy>(spy);
        registeredSpies.add(spyReference);
        return spyReference;
    }

    synchronized void removeSpyReferenceImpl(WeakReference<Spy> spyReference) {
        registeredSpies.remove(spyReference);
    }

    synchronized void notifyListeners(String sql) {
        Iterator<WeakReference<Spy>> iterator = registeredSpies.iterator();
        while (iterator.hasNext()) {
            WeakReference<Spy> spyReference = iterator.next();
            Spy spy = spyReference.get();
            if (null == spy) {
                iterator.remove();
            } else {
                spy.addExecutedSql(sql);
            }
        }
    }

    int executeStatementImpl(Query.Type queryType) {
        switch (queryType) {
            case SELECT:
                return counterSelect.incrementAndGet();
            case INSERT:
                return counterInsert.incrementAndGet();
            case UPDATE:
                return counterUpdate.incrementAndGet();
            case DELETE:
                return counterDelete.incrementAndGet();
            case OTHER:
            default:
                return counterOther.incrementAndGet();
        }
    }

    static WeakReference<Spy> registerSpy(Spy spy) {
        return INSTANCE.registerSpyImpl(spy);
    }

    static void removeSpyReference(WeakReference<Spy> spyReference) {
        INSTANCE.removeSpyReferenceImpl(spyReference);
    }

    static void executeStatement(String sql, long nanos) {
        QueryLogger.logQuery(sql, nanos);
        Query query = Query.parse(sql);
        INSTANCE.executeStatementImpl(query.type);
        INSTANCE.notifyListeners(sql);
        ThreadLocalSniffer.executeStatement(query.type);
    }

    static List<WeakReference<Spy>> registeredSpies() {
        return Collections.unmodifiableList(INSTANCE.registeredSpies);
    }

    int executedStatementsImpl(Query.Type queryType) {
        switch (queryType) {
            case ALL:
                return counterSelect.get() + counterInsert.get() + counterUpdate.get() + counterDelete.get() + counterOther.get();
            case SELECT:
                return counterSelect.get();
            case INSERT:
                return counterInsert.get();
            case UPDATE:
                return counterUpdate.get();
            case DELETE:
                return counterDelete.get();
            case OTHER:
            default:
                return counterOther.get();
        }
    }

    int executedStatementsImpl() {
        return executedStatementsImpl(Query.Type.ALL);
    }

    /**
     * @return number of SQL statements executed by current thread since some fixed moment of time
     * @since 1.0
     */
    public static int executedStatements() {
        return INSTANCE.executedStatementsImpl();
    }

    /**
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    public static Spy spy() {
        return new Spy();
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
     * @see Spy#expectNever(Query.Type)
     * @since 2.2
     */
    public static Spy expectNever(Query.Type queryType) {
        return spy().expectNever(queryType);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectNever(Threads,Query.Type)
     * @since 2.2
     */
    public static Spy expectNever(Threads threadMatcher, Query.Type queryType) {
        return spy().expectNever(threadMatcher, queryType);
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
     * @see Spy#expectAtMostOnce(Query.Type)
     * @since 2.2
     */
    public static Spy expectAtMostOnce(Query.Type queryType) {
        return spy().expectAtMostOnce(queryType);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMostOnce(Threads,Query.Type)
     * @since 2.2
     */
    public static Spy expectAtMostOnce(Threads threadMatcher, Query.Type queryType) {
        return spy().expectAtMostOnce(threadMatcher, queryType);
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
     * @see Spy#expectAtMost(int, Query.Type)
     * @since 2.2
     */
    public static Spy expectAtMost(int allowedStatements, Query.Type queryType) {
        return spy().expectAtMost(allowedStatements, queryType);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtMost(int, Threads, Query.Type)
     * @since 2.2
     */
    public static Spy expectAtMost(int allowedStatements, Threads threadMatcher, Query.Type queryType) {
        return spy().expectAtMost(allowedStatements, threadMatcher, queryType);
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
     * @see Spy#expect(int, Query.Type)
     * @since 2.2
     */
    public static Spy expect(int allowedStatements, Query.Type queryType) {
        return spy().expect(allowedStatements, queryType);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expect(int, Threads, Query.Type)
     * @since 2.2
     */
    public static Spy expect(int allowedStatements, Threads threadMatcher, Query.Type queryType) {
        return spy().expect(allowedStatements, threadMatcher, queryType);
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
     * @see Spy#expectAtLeast(int, Query.Type)
     * @since 2.2
     */
    public static Spy expectAtLeast(int allowedStatements, Query.Type queryType) {
        return spy().expectAtLeast(allowedStatements, queryType);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectAtLeast(int, Threads, Query.Type)
     * @since 2.2
     */
    public static Spy expectAtLeast(int allowedStatements, Threads threadMatcher, Query.Type queryType) {
        return spy().expectAtLeast(allowedStatements, threadMatcher, queryType);
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
     * @see Spy#expectBetween(int, int, Query.Type)
     * @since 2.0
     */
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Query.Type queryType) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, queryType);
    }

    /**
     * @return a new {@link Spy} instance with an expectation initialized
     * @see #spy()
     * @see Spy#expectBetween(int, int, Threads, Query.Type)
     * @since 2.0
     */
    public static Spy expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query.Type queryType) {
        return spy().expectBetween(minAllowedStatements, maxAllowedStatements, threadMatcher, queryType);
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
                spy.expect(expectation.value(), expectation.threads());
            }
            if (-1 != expectation.atLeast() && -1 != expectation.atMost()) {
                spy.expectBetween(expectation.atLeast(), expectation.atMost(), expectation.threads());
            } else if (-1 != expectation.atLeast()) {
                spy.expectAtLeast(expectation.atLeast(), expectation.threads());
            } else if (-1 != expectation.atMost()) {
                spy.expectAtMost(expectation.atMost(), expectation.threads());
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
         * When {@link com.github.bedrin.jdbc.sniffer.Sniffer#execute(com.github.bedrin.jdbc.sniffer.Sniffer.Executable)}
         * method is called, it will execute the Executable.execute() method, record the SQL queries and return the
         * {@link Spy} object with stats
         * @throws Exception code under test can throw any exception
         */
        void execute() throws Throwable;

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

    protected final static Threads DEFAULT_THREAD_MATCHER = Threads.CURRENT;

    static class ThreadLocalSniffer extends ThreadLocal<Sniffer> {

        private final static ThreadLocalSniffer INSTANCE = new ThreadLocalSniffer();

        @Override
        protected Sniffer initialValue() {
            return new Sniffer();
        }

        static Sniffer getSniffer() {
            return INSTANCE.get();
        }

        static void executeStatement(Query.Type queryType) {
            getSniffer().executeStatementImpl(queryType);
        }

        static int executedStatements(Query.Type queryType) {
            return getSniffer().executedStatementsImpl(queryType);
        }

        static int executedStatements() {
            return getSniffer().executedStatementsImpl();
        }

    }

}