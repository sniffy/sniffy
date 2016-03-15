package io.sniffy;

import io.sniffy.socket.SocketStats;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.ExceptionUtil;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static io.sniffy.Sniffer.DEFAULT_THREAD_MATCHER;
import static io.sniffy.util.ExceptionUtil.throwException;

/**
 * Spy holds a number of queries which were executed at some point of time and uses it as a base for further assertions
 * @see Sniffer#spy()
 * @see Sniffer#expect(int)
 * @since 2.0
 */
public class Spy<C extends Spy<C>> implements Closeable {

    private Counter initialCount;
    private Counter initialThreadLocalCount;

    private volatile Collection<StatementMetaData> executedStatements = new ConcurrentLinkedQueue<StatementMetaData>();
    private final WeakReference<Spy> selfReference;
    private final Thread owner;

    private boolean closed = false;
    private StackTraceElement[] closeStackTrace;

    private volatile ConcurrentMap<Thread, ConcurrentMap<String, SocketStats>> socketOperations =
            new ConcurrentHashMap<Thread, ConcurrentMap<String, SocketStats>>();

    protected void addExecutedStatement(StatementMetaData statementMetaData) {
        executedStatements.add(statementMetaData);
    }

    protected void addExecutedStatement(String address, SocketStats socketStats) {

        Thread currentThread = Thread.currentThread();

        ConcurrentMap<String, SocketStats> threadSocketOperations = new ConcurrentHashMap<String, SocketStats>();
        ConcurrentMap<String, SocketStats> existingThreadSocketOperations = socketOperations.putIfAbsent(
                currentThread, threadSocketOperations
        );
        if (null != existingThreadSocketOperations) {
            threadSocketOperations = existingThreadSocketOperations;
        }

        SocketStats existingSocketStats = threadSocketOperations.putIfAbsent(address, socketStats);
        if (null != existingSocketStats) {
            existingSocketStats.inc(socketStats);
        }

    }

    public Map<String, SocketStats> getSocketOperations() {
        ConcurrentMap<String, SocketStats> threadSocketOperations = socketOperations.get(Thread.currentThread());
        return Collections.unmodifiableMap(null == threadSocketOperations ? Collections.<String, SocketStats>emptyMap() : threadSocketOperations);
    }

    protected void resetExecutedStatements() {
        executedStatements = new ConcurrentLinkedQueue<StatementMetaData>();
    }

    public List<StatementMetaData> getExecutedStatements(Threads threadMatcher) {
        List<StatementMetaData> statements;
        switch (threadMatcher) {
            case CURRENT:
                statements = new ArrayList<StatementMetaData>();
                for (StatementMetaData statement : executedStatements) {
                    if (statement.owner == this.owner) {
                        statements.add(statement);
                    }
                }
                break;
            case OTHERS:
                statements = new ArrayList<StatementMetaData>();
                for (StatementMetaData statement : executedStatements) {
                    if (statement.owner == this.owner) {
                        statements.add(statement);
                    }
                }
                break;
            case ANY:
            default:
                statements = new ArrayList<StatementMetaData>(executedStatements);
                break;
        }
        return Collections.unmodifiableList(statements);
    }

    Spy() {
        owner = Thread.currentThread();
        selfReference = Sniffer.registerSpy(this);
        reset();
    }

    private void initNumberOfQueries() {
        this.initialCount = new Counter(Sniffer.COUNTER);
        this.initialThreadLocalCount = new Counter(Sniffer.THREAD_LOCAL_COUNTER.get());
    }

    private List<Expectation> expectations = new ArrayList<Expectation>();

    /**
     * Wrapper for {@link Sniffer#spy()} method; useful for chaining
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    public Spy reset() {
        checkOpened();
        initNumberOfQueries();
        resetExecutedStatements();
        expectations.clear();
        return self();
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
    public int executedStatements(Threads threadMatcher) {
        return executedStatements(threadMatcher, Query.ANY);
    }

    /**
     * @param threadMatcher chooses {@link Thread}s for calculating the number of executed queries
     * @return number of SQL statements executed since some fixed moment of time
     * @since 2.2
     */
    public int executedStatements(Threads threadMatcher, Query query) {

        checkOpened();

        switch (threadMatcher) {
            case ANY:
                return Sniffer.COUNTER.executedStatements(query) - initialCount.executedStatements(query);
            case CURRENT:
                return Sniffer.THREAD_LOCAL_COUNTER.get().executedStatements(query) - initialThreadLocalCount.executedStatements(query);
            case OTHERS:
                return Sniffer.COUNTER.executedStatements(query) - Sniffer.THREAD_LOCAL_COUNTER.get().executedStatements(query)
                        - initialCount.executedStatements(query) + initialThreadLocalCount.executedStatements(query);
            default:
                throw new IllegalArgumentException(String.format("Unknown thread matcher %s", threadMatcher.getClass().getName()));
        }

    }

    // never methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C expectNever() {
        checkOpened();
        return expectNever(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C expectNever(Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 0, threadMatcher, Query.ANY));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expectNever(Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 0, DEFAULT_THREAD_MATCHER, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectNever(Threads threadMatcher, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 0, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectNever(Query query, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 0, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C verifyNever() throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyNever(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C verifyNever(Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 0, threadMatcher, Query.ANY).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verifyNever(Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 0, DEFAULT_THREAD_MATCHER, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyNever(Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 0, threadMatcher, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyNever(Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 0, threadMatcher, query).validate();
        return self();
    }

    // atMostOnce methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C expectAtMostOnce() {
        checkOpened();
        return expectAtMostOnce(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C expectAtMostOnce(Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 1, threadMatcher, Query.ANY));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMostOnce(Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 1, DEFAULT_THREAD_MATCHER, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMostOnce(Threads threadMatcher, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 1, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMostOnce(Query query, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 1, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C verifyAtMostOnce() throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyAtMostOnce(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C verifyAtMostOnce(Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 1, threadMatcher, Query.ANY).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMostOnce(Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 1, DEFAULT_THREAD_MATCHER, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMostOnce(Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 1, threadMatcher, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMostOnce(Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 1, threadMatcher, query).validate();
        return self();
    }

    // atMost methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C expectAtMost(int allowedStatements) {
        checkOpened();
        return expectAtMost(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C expectAtMost(int allowedStatements, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, Query.ANY));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMost(int allowedStatements, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, DEFAULT_THREAD_MATCHER, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMost(int allowedStatements, Threads threadMatcher, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMost(int allowedStatements, Query query, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C verifyAtMost(int allowedStatements) throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyAtMost(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C verifyAtMost(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, Query.ANY).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMost(int allowedStatements, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, allowedStatements, DEFAULT_THREAD_MATCHER, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMost(int allowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMost(int allowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, query).validate();
        return self();
    }

    // exact methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C expect(int allowedStatements) {
        checkOpened();
        return expect(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C expect(int allowedStatements, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, Query.ANY));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expect(int allowedStatements, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, DEFAULT_THREAD_MATCHER, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expect(int allowedStatements, Threads threadMatcher, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expect(int allowedStatements, Query query, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C verify(int allowedStatements) throws WrongNumberOfQueriesError {
        checkOpened();
        return verify(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C verify(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, Query.ANY).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verify(int allowedStatements, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, DEFAULT_THREAD_MATCHER, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verify(int allowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verify(int allowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, query).validate();
        return self();
    }

    // atLeast methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C expectAtLeast(int allowedStatements) {
        checkOpened();
        return expectAtLeast(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C expectAtLeast(int allowedStatements, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, Query.ANY));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expectAtLeast(int allowedStatements, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, DEFAULT_THREAD_MATCHER, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtLeast(int allowedStatements, Threads threadMatcher, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtLeast(int allowedStatements, Query query, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C verifyAtLeast(int allowedStatements) throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyAtLeast(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    public C verifyAtLeast(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, Query.ANY).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtLeast(int allowedStatements, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, DEFAULT_THREAD_MATCHER, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtLeast(int allowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, query).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtLeast(int allowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, query).validate();
        return self();
    }

    // between methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code minAllowedStatements}, {@code maxAllowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements) {
        checkOpened();
        return expectBetween(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.0
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, Query.ANY));
        return self();
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER, query));
        return self();
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query query) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, query));
        return self();
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, query));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments {@code minAllowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements) throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyBetween(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, Query.ANY).validate();
        return self();
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER, query).validate();
        return self();
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, query).validate();
        return self();
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, query).validate();
        return self();
    }

    // end

    /**
     * Verifies all expectations added previously using {@code expect} methods family
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public void verify() throws WrongNumberOfQueriesError {
        checkOpened();
        WrongNumberOfQueriesError assertionError = getWrongNumberOfQueriesError();
        if (null != assertionError) {
            throw assertionError;
        }
    }

    /**
     *
     * @return WrongNumberOfQueriesError or null if there are no errors
     * @since 2.1
     */
    public WrongNumberOfQueriesError getWrongNumberOfQueriesError() {
        checkOpened();
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
        return assertionError;
    }

    /**
     * Alias for {@link #verify()} method; it is useful for try-with-resource API:
     * <pre>
     * <code>
     *     {@literal @}Test
     *     public void testTryWithResourceApi() throws SQLException {
     *         final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
     *         try (@SuppressWarnings("unused") Spy s = Sniffer.expectAtMostOnce();
     *              Statement statement = connection.createStatement()) {
     *             statement.execute("SELECT 1 FROM DUAL");
     *         }
     *     }
     * }
     * </code>
     * </pre>
     * @since 2.0
     */
    public void close() {
        try {
            verify();
        } finally {
            Sniffer.removeSpyReference(selfReference);
            closed = true;
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            closeStackTrace = new StackTraceElement[stackTrace.length - 1];
            System.arraycopy(stackTrace, 1, closeStackTrace, 0, stackTrace.length - 1);
        }
    }

    private void checkOpened() {
        if (closed) {
            throw new SpyClosedException("Spy is closed", closeStackTrace);
        }
    }

    /**
     * Executes the {@link Sniffer.Executable#execute()} method on provided argument and verifies the expectations
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public C execute(Sniffer.Executable executable) {
        checkOpened();
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
        checkOpened();
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
        checkOpened();
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
            if (!ExceptionUtil.addSuppressed(e, ae)) {
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
        private final Threads threadMatcher;
        private final Query query;

        public ThreadMatcherExpectation(int minimumQueries, int maximumQueries, Threads threadMatcher, Query query) {
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
            this.threadMatcher = threadMatcher;
            this.query = query;
        }

        public void validate() throws WrongNumberOfQueriesError {

            int numQueries = executedStatements(threadMatcher, query);

            if (numQueries > maximumQueries || numQueries < minimumQueries) {
                throw new WrongNumberOfQueriesError(
                        threadMatcher,
                        minimumQueries, maximumQueries, numQueries,
                        getExecutedStatements(threadMatcher)
                );
            }

        }

    }

    @SuppressWarnings("unchecked")
    private C self() {
        return (C) this;
    }

}
