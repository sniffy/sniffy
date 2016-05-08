package io.sniffy;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlQueries;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.ExceptionUtil;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Callable;

import static io.sniffy.Threads.*;
import static io.sniffy.util.ExceptionUtil.throwException;

/**
 * Spy holds a number of queries which were executed at some point of time and uses it as a base for further assertions
 * @see Sniffer#spy()
 * @see Sniffer#expect(int)
 * @since 2.0
 */
public class Spy<C extends Spy<C>> implements Closeable {

    // TODO: add invocationcount to executedStatements and socketOperations; collapse similar queries
    private volatile ConcurrentLinkedHashMap<StatementMetaData, SqlStats> executedStatements =
            new ConcurrentLinkedHashMap.Builder<StatementMetaData, SqlStats>().
                    maximumWeightedCapacity(Long.MAX_VALUE).
                    build();

    private volatile ConcurrentLinkedHashMap<SocketMetaData, SocketStats> socketOperations =
            new ConcurrentLinkedHashMap.Builder<SocketMetaData, SocketStats>().
                    maximumWeightedCapacity(Long.MAX_VALUE).
                    build();

    private final WeakReference<Spy> selfReference;
    private final long ownerThreadId;
    private final boolean spyCurrentThreadOnly;

    private boolean closed = false;
    private StackTraceElement[] closeStackTrace;

    private List<Expectation> expectations = new ArrayList<Expectation>();

    protected void addExecutedStatement(StatementMetaData statementMetaData, long elapsedTime, int bytesDown, int bytesUp) {
        if (!spyCurrentThreadOnly || ownerThreadId == statementMetaData.ownerThreadId) {
            SqlStats sqlStats = executedStatements.get(statementMetaData);
            if (null == sqlStats) {
                sqlStats = executedStatements.putIfAbsent(statementMetaData, new SqlStats(elapsedTime, bytesDown, bytesUp, 0));
            }
            if (null != sqlStats) {
                sqlStats.accumulate(elapsedTime, bytesDown, bytesUp);
            }
        }
    }

    protected void addReturnedRow(StatementMetaData statementMetaData) {
        if (!spyCurrentThreadOnly || ownerThreadId == statementMetaData.ownerThreadId) {
            SqlStats sqlStats = executedStatements.get(statementMetaData);
            if (null != sqlStats) {
                sqlStats.accumulate(0, 0, 0, 1);
            }
        }
    }

    protected void addSocketOperation(SocketMetaData socketMetaData, long elapsedTime, int bytesDown, int bytesUp) {
        if (!spyCurrentThreadOnly || ownerThreadId == socketMetaData.ownerThreadId) {
            SocketStats socketStats = socketOperations.get(socketMetaData);
            if (null == socketStats) {
                socketStats = socketOperations.putIfAbsent(socketMetaData, new SocketStats(elapsedTime, bytesDown, bytesUp));
            }
            if (null != socketStats) {
                socketStats.accumulate(elapsedTime, bytesDown, bytesUp);
            }
        }
    }

    protected void resetExecutedStatements() {
        executedStatements =
                new ConcurrentLinkedHashMap.Builder<StatementMetaData, SqlStats>().
                        maximumWeightedCapacity(Long.MAX_VALUE).
                        build();
    }

    protected void resetSocketOpertions() {
        socketOperations = new ConcurrentLinkedHashMap.Builder<SocketMetaData, SocketStats>().
                maximumWeightedCapacity(Long.MAX_VALUE).
                build();
    }

    public Map<StatementMetaData, SqlStats> getExecutedStatements(Threads threadMatcher, boolean removeStackTraces) {

        Map<StatementMetaData, SqlStats> executedStatements = new LinkedHashMap<StatementMetaData, SqlStats>();
        for (Map.Entry<StatementMetaData, SqlStats> entry : this.executedStatements.ascendingMap().entrySet()) {

            StatementMetaData statementMetaData = entry.getKey();

            if (removeStackTraces) statementMetaData = new StatementMetaData(
                    statementMetaData.sql, statementMetaData.query, null, statementMetaData.ownerThreadId
            );

            if ( ( (CURRENT == threadMatcher && statementMetaData.ownerThreadId == this.ownerThreadId) ||
                    (OTHERS == threadMatcher && statementMetaData.ownerThreadId != this.ownerThreadId) ||
                    (ANY == threadMatcher || null == threadMatcher) ) ) {
                SqlStats existingSocketStats = executedStatements.get(statementMetaData);
                if (null == existingSocketStats) {
                    executedStatements.put(statementMetaData, new SqlStats(entry.getValue()));
                } else {
                    existingSocketStats.accumulate(entry.getValue());
                }
            }
        }

        return Collections.unmodifiableMap(executedStatements);
    }

    Spy(boolean spyCurrentThreadOnly) {
        ownerThreadId = Thread.currentThread().getId();
        selfReference = Sniffer.registerSpy(this);
        this.spyCurrentThreadOnly = spyCurrentThreadOnly;
        reset();
    }

    /**
     * Wrapper for {@link Sniffer#spy()} method; useful for chaining
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    public Spy reset() {
        checkOpened();
        resetExecutedStatements();
        resetSocketOpertions();
        expectations.clear();
        return self();
    }

    public Map<SocketMetaData, SocketStats> getSocketOperations(Threads threadMatcher, String address, boolean removeStackTraces) {

        String hostName = null;
        Integer port = null;

        if (null != address) {
            if (-1 != address.indexOf(':')) {
                String[] split = address.split(":");
                hostName = split[0];
                port = Integer.valueOf(split[1]);
            } else {
                hostName = address;
            }
        }

        Map<SocketMetaData, SocketStats> socketOperations = new LinkedHashMap<SocketMetaData, SocketStats>();
        for (Map.Entry<SocketMetaData, SocketStats> entry : this.socketOperations.ascendingMap().entrySet()) {

            SocketMetaData socketMetaData = entry.getKey();

            if (removeStackTraces) socketMetaData = new SocketMetaData(
                    socketMetaData.address, socketMetaData.connectionId, null, socketMetaData.ownerThreadId
            );

            InetSocketAddress socketAddress = socketMetaData.address;
            InetAddress inetAddress = socketAddress.getAddress();

            if ( ( (CURRENT == threadMatcher && socketMetaData.ownerThreadId == this.ownerThreadId) ||
                    (OTHERS == threadMatcher && socketMetaData.ownerThreadId != this.ownerThreadId) ||
                    (ANY == threadMatcher || null == threadMatcher) ) &&
                    (null == hostName || hostName.equals(inetAddress.getHostName()) || hostName.equals(inetAddress.getHostAddress()) || hostName.equals(inetAddress.getCanonicalHostName())) &&
                    (null == port || port == socketAddress.getPort())
                    ) {
                SocketStats existingSocketStats = socketOperations.get(socketMetaData);
                if (null == existingSocketStats) {
                    socketOperations.put(socketMetaData, new SocketStats(entry.getValue()));
                } else {
                    existingSocketStats.accumulate(entry.getValue());
                }
            }
        }

        return Collections.unmodifiableMap(socketOperations);

    }

    /**
     * @return number of SQL statements executed by current thread since some fixed moment of time
     * @since 2.0
     */
    public int executedStatements() {
        return executedStatements(CURRENT);
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

        int count = 0;

        switch (threadMatcher) {
            case ANY:
                if (query == Query.ANY) count = executedStatements.size();
                else for (StatementMetaData statementMetaData : executedStatements.keySet()) {
                    if (query == statementMetaData.query) count++;
                }
                break;
            case CURRENT:
            case OTHERS:
                for (StatementMetaData statementMetaData : executedStatements.keySet()) {
                    if ((Thread.currentThread().getId() == statementMetaData.ownerThreadId) == (CURRENT == threadMatcher) &&
                            (query == Query.ANY || query == statementMetaData.query)) count++;
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown thread matcher %s", threadMatcher.getClass().getName()));
        }

        return count;

    }


    // Expect and verify methods


    /**
     * @param expectation
     * @return
     * @since 3.1
     */
    public C expect(Expectation expectation) {
        checkOpened();
        expectations.add(expectation);
        return self();
    }

    /**
     * @param expectation
     * @return
     * @since 3.1
     */
    public C verify(Expectation expectation) {
        checkOpened();
        expectation.verify(this);
        return self();
    }

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
                expectation.verify(this);
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

    public interface Expectation {

        <T extends Spy<T>> Spy<T> verify(Spy<T> spy) throws SniffyAssertionError;

    }

    @SuppressWarnings("unchecked")
    private C self() {
        return (C) this;
    }









    // DEPRECATED API





    // never methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectNever() {
        return expect(SqlQueries.noneQueries());
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectNever(Threads threadMatcher) {
        return expect(SqlQueries.noneQueries().threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectNever(Query query) {
        return expect(SqlQueries.noneQueries().type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectNever(Threads threadMatcher, Query query) {
        return expect(SqlQueries.noneQueries().threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectNever(Query query, Threads threadMatcher) {
        return expect(SqlQueries.noneQueries().type(query).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyNever() throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries());
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyNever(Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries().threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyNever(Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries().type(query));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyNever(Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries().threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyNever(Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries().type(query).threads(threadMatcher));
    }

    // atMostOnce methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtMostOnce() {
        return expect(SqlQueries.atMostOneQuery());
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtMostOnce(Threads threadMatcher) {
        return expect(SqlQueries.atMostOneQuery().threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMostOnce(Query query) {
        return expect(SqlQueries.atMostOneQuery().type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMostOnce(Threads threadMatcher, Query query) {
        return expect(SqlQueries.atMostOneQuery().threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMostOnce(Query query, Threads threadMatcher) {
        return expect(SqlQueries.atMostOneQuery().type(query).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtMostOnce() throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery());
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtMostOnce(Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery().threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMostOnce(Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery().type(query));

    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMostOnce(Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery().threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMostOnce(Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery().type(query).threads(threadMatcher));
    }

    // atMost methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtMost(int allowedStatements) {
        return expect(SqlQueries.maxQueries(allowedStatements));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtMost(int allowedStatements, Threads threadMatcher) {
        return expect(SqlQueries.maxQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMost(int allowedStatements, Query query) {
        return expect(SqlQueries.maxQueries(allowedStatements).type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMost(int allowedStatements, Threads threadMatcher, Query query) {
        return expect(SqlQueries.maxQueries(allowedStatements).threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMost(int allowedStatements, Query query, Threads threadMatcher) {
        return expect(SqlQueries.maxQueries(allowedStatements).type(query).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements).type(query));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements).threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements).type(query).threads(threadMatcher));
    }

    // exact methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expect(int allowedStatements) {
        return expect(SqlQueries.exactQueries(allowedStatements));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expect(int allowedStatements, Threads threadMatcher) {
        return expect(SqlQueries.exactQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expect(int allowedStatements, Query query) {
        return expect(SqlQueries.exactQueries(allowedStatements).type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expect(int allowedStatements, Threads threadMatcher, Query query) {
        return expect(SqlQueries.exactQueries(allowedStatements).threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expect(int allowedStatements, Query query, Threads threadMatcher) {
        return expect(SqlQueries.exactQueries(allowedStatements).type(query).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verify(int allowedStatements) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verify(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verify(int allowedStatements, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements).type(query));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verify(int allowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements).threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verify(int allowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements).type(query).threads(threadMatcher));
    }

    // atLeast methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements) {
        return expect(SqlQueries.minQueries(allowedStatements));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements, Threads threadMatcher) {
        return expect(SqlQueries.minQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements, Query query) {
        return expect(SqlQueries.minQueries(allowedStatements).type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements, Threads threadMatcher, Query query) {
        return expect(SqlQueries.minQueries(allowedStatements).threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements, Query query, Threads threadMatcher) {
        return expect(SqlQueries.minQueries(allowedStatements).type(query).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements).type(query));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements).threads(threadMatcher).type(query));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements).type(query).threads(threadMatcher));
    }

    // between methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code minAllowedStatements}, {@code maxAllowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements));
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.0
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).threads(threadMatcher));
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).type(query));
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query query) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).threads(threadMatcher).type(query));
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query, Threads threadMatcher) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).type(query).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments {@code minAllowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements));
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).threads(threadMatcher));
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).type(query));
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).threads(threadMatcher).type(query));
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).type(query).threads(threadMatcher));
    }

}
