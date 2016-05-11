package io.sniffy;

import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.StatementMetaData;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static io.sniffy.util.StackTraceExtractor.getTraceForProxiedMethod;
import static io.sniffy.util.StackTraceExtractor.printStackTrace;

/**
 * @since 3.1
 */
public class Sniffy {

    private static final List<WeakReference<Spy>> registeredSpies = new LinkedList<WeakReference<Spy>>();
    private static ThreadLocal<SocketStats> socketStatsAccumulator = new ThreadLocal<SocketStats>();

    protected Sniffy() {
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
        Sniffer.executedStatementsGlobalCounter.incrementAndGet();

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
     * @return a new {@link Spy} instance for currenth thread only
     * @since 3.1
     */
    public static <T extends Spy<T>> Spy<? extends Spy<T>> spyCurrentThread() {
        return new Spy<T>(true);
    }

    /**
     * @param expectation a {@link Spy.Expectation} implementation
     * @return a new {@link Spy} instance with given expectation
     * @see #spy()
     * @since 3.1
     */
    public static Spy expect(Spy.Expectation expectation) {
        return spy().expect(expectation);
    }

    /**
     * Execute the {@link Executable#execute()} method, record the SQL queries
     * and return the {@link Spy} object with stats
     * @param executable code to test
     * @return statistics on executed queries
     * @throws RuntimeException if underlying code under test throws an Exception
     * @since 3.1
     */
    public static Spy execute(Executable executable) {
        return spy().execute(executable);
    }

    /**
     * Execute the {@link Runnable#run()} method, record the SQL queries
     * and return the {@link Spy} object with stats
     * @param runnable code to test
     * @return statistics on executed queries
     * @since 3.1
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
     * @since 3.1
     */
    @SuppressWarnings("unchecked")
    public static <V> SpyWithValue<V> call(Callable<V> callable) throws Exception {
        return spy().call(callable);
    }
}
