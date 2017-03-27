package io.sniffy;

import com.codahale.metrics.Timer;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStatement;
import io.sniffy.sql.SqlUtil;
import io.sniffy.sql.StatementMetaData;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

import static io.sniffy.util.StackTraceExtractor.*;

/**
 * Sniffy allows you to validate the number of SQL queries executed by a given block of code
 * Example usage:
 * <pre>
 * <code>
 *     Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa");
 *     Spy{@literal <}?{@literal >} spy = Sniffy.spy();
 *     connection.createStatement().execute("SELECT 1 FROM DUAL");
 *     spy.verify(SqlQueries.atMostOneQuery());
 * </code>
 * </pre>
 * @since 3.1
 */
public class Sniffy {

    public static final int TOP_SQL_CAPACITY = 1024;

    protected static final Queue<WeakReference<Spy>> registeredSpies =
            new ConcurrentLinkedQueue<WeakReference<Spy>>();
    protected static final ConcurrentMap<Long, WeakReference<CurrentThreadSpy>> currentThreadSpies =
            new ConcurrentHashMap<Long, WeakReference<CurrentThreadSpy>>();
    protected static volatile ConcurrentLinkedHashMap<String, Timer> globalSqlStats =
            new ConcurrentLinkedHashMap.Builder<String, Timer>().
                    maximumWeightedCapacity(SniffyConfiguration.INSTANCE.getTopSqlCapacity()).
                    build();

    private static ThreadLocal<SocketStats> socketStatsAccumulator = new ThreadLocal<SocketStats>();

    private static volatile boolean initialized = false;

    protected Sniffy() {
    }

    static {
        initialize();
    }

    /**
     * If socket monitoring is already enabled it cannot be disabled afterwards
     * Otherwise one webapp would enable it but another one would disable
     */
    public static void initialize() {

        if (initialized) return;

        SniffyConfiguration.INSTANCE.addTopSqlCapacityListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ConcurrentLinkedHashMap<String, Timer> oldValues = globalSqlStats;
                globalSqlStats =
                        new ConcurrentLinkedHashMap.Builder<String, Timer>().
                                maximumWeightedCapacity(SniffyConfiguration.INSTANCE.getTopSqlCapacity()).
                                build();
                globalSqlStats.putAll(oldValues);
            }

        });


        if (SniffyConfiguration.INSTANCE.isMonitorSocket()) {

            try {
                SnifferSocketImplFactory.install();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {

            SniffyConfiguration.INSTANCE.addMonitorSocketListener(new PropertyChangeListener() {

                private boolean sniffySocketImplFactoryInstalled = false;

                @Override
                public synchronized void propertyChange(PropertyChangeEvent evt) {
                    if (sniffySocketImplFactoryInstalled) return;
                    if (Boolean.TRUE.equals(evt.getNewValue())) {
                        try {
                            SnifferSocketImplFactory.install();
                            sniffySocketImplFactoryInstalled = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            });

        }

        initialized = true;

    }

    // TODO: call this method via Disruptor or something
    public static void logSqlTime(String sql, long elapsedTime) {
        if (SniffyConfiguration.INSTANCE.getTopSqlCapacity() <= 0) return;
        String normalizedSql = SqlUtil.normalizeInStatement(sql);
        Timer timer = globalSqlStats.get(normalizedSql);
        if (null == timer) {
            Timer newTimer = new Timer();
            newTimer.update(elapsedTime, TimeUnit.MILLISECONDS);
            timer = globalSqlStats.putIfAbsent(normalizedSql, newTimer);
        }
        if (null != timer) {
            timer.update(elapsedTime, TimeUnit.MILLISECONDS);
        }
    }

    public static ConcurrentMap<String, Timer> getGlobalSqlStats() {
        return globalSqlStats;
    }

    protected static WeakReference<Spy> registerSpy(Spy spy) {
        WeakReference<Spy> spyReference = new WeakReference<Spy>(spy);
        registeredSpies.add(spyReference);
        return spyReference;
    }

    protected static WeakReference<CurrentThreadSpy> registerCurrentThreadSpy(CurrentThreadSpy spy) {
        WeakReference<CurrentThreadSpy> spyReference = new WeakReference<CurrentThreadSpy>(spy);
        currentThreadSpies.put(Thread.currentThread().getId(), spyReference);
        return spyReference;
    }

    protected static void removeSpyReference(WeakReference<Spy> spyReference) {
        registeredSpies.remove(spyReference);
    }

    protected static void removeCurrentThreadSpyReference() {
        currentThreadSpies.remove(Thread.currentThread().getId());
    }

    private static void notifyListeners(StatementMetaData statementMetaData, long elapsedTime, int bytesDown, int bytesUp, int rowsUpdated) {
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

        Long threadId = Thread.currentThread().getId();

        WeakReference<CurrentThreadSpy> spyReference = currentThreadSpies.get(threadId);
        if (null != spyReference) {
            CurrentThreadSpy spy = spyReference.get();
            if (null == spy) {
                currentThreadSpies.remove(threadId);
            } else {
                spy.addExecutedStatement(statementMetaData, elapsedTime, bytesDown, bytesUp, rowsUpdated);
            }
        }

    }

    private static void notifyListeners(StatementMetaData statementMetaData) {
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

        Long threadId = Thread.currentThread().getId();

        WeakReference<CurrentThreadSpy> spyReference = currentThreadSpies.get(threadId);
        if (null != spyReference) {
            CurrentThreadSpy spy = spyReference.get();
            if (null == spy) {
                currentThreadSpies.remove(threadId);
            } else {
                spy.addReturnedRow(statementMetaData);
            }
        }
    }

    private static void notifyListeners(SocketMetaData socketMetaData, long elapsedTime, int bytesDown, int bytesUp) {
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

        Long threadId = Thread.currentThread().getId();

        WeakReference<CurrentThreadSpy> spyReference = currentThreadSpies.get(threadId);
        if (null != spyReference) {
            CurrentThreadSpy spy = spyReference.get();
            if (null == spy) {
                currentThreadSpies.remove(threadId);
            } else {
                spy.addSocketOperation(socketMetaData, elapsedTime, bytesDown, bytesUp);
            }
        }
    }

    public static boolean hasSpies() {
        if (!registeredSpies.isEmpty()) {
            Iterator<WeakReference<Spy>> iterator = registeredSpies.iterator();
            while (iterator.hasNext()) {
                WeakReference<Spy> spyReference = iterator.next();
                Spy spy = spyReference.get();
                if (null == spy) {
                    iterator.remove();
                } else {
                    return true;
                }
            }
        }

        Long threadId = Thread.currentThread().getId();

        WeakReference<CurrentThreadSpy> spyReference = currentThreadSpies.get(threadId);
        if (null != spyReference) {
            CurrentThreadSpy spy = spyReference.get();
            if (null == spy) {
                currentThreadSpies.remove(threadId);
            } else {
                return true;
            }
        }

        return false;
    }

    public static void logSocket(int connectionId, InetSocketAddress address, long elapsedTime, int bytesDown, int bytesUp) {

        // do not track JDBC socket operations
        SocketStats socketStats = socketStatsAccumulator.get();
        if (null != socketStats) {
            socketStats.accumulate(elapsedTime, bytesDown, bytesUp);
        } else {
            // build stackTrace
            String stackTrace = printStackTrace(getTraceTillPackage("java.net"));

            // increment counters
            SocketMetaData socketMetaData = new SocketMetaData(address, connectionId, stackTrace, Thread.currentThread().getId());

            // notify listeners
            notifyListeners(socketMetaData, elapsedTime, bytesDown, bytesUp);
        }
    }

    public static void enterJdbcMethod() {
        // TODO: check if it supports reentrant calls
        socketStatsAccumulator.set(new SocketStats(0, 0, 0));
    }

    public static void exitJdbcMethod(Method method, long elapsedTime) {
        exitJdbcMethod(method, elapsedTime, null);
    }

    public static void exitJdbcMethod(Method method, long elapsedTime, Method implMethod) {
        if (Sniffy.hasSpies()) {
            // get accumulated socket stats
            SocketStats socketStats = socketStatsAccumulator.get();

            if (null != socketStats) {

                if (socketStats.bytesDown.longValue() > 0 || socketStats.bytesUp.longValue() > 0) {
                    String stackTrace = null;
                    try {
                        stackTrace = printStackTrace(null == implMethod ?
                                getTraceForProxiedMethod(method) :
                                getTraceForImplementingMethod(method, implMethod)
                        );
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    StatementMetaData statementMetaData = new StatementMetaData(
                            method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()",
                            SqlStatement.SYSTEM,
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

            }
        }
        socketStatsAccumulator.remove();
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
        StatementMetaData statementMetaData = new StatementMetaData(sql, SqlUtil.guessQueryType(sql), stackTrace, Thread.currentThread().getId());
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
        return new Spy<T>();
    }

    /**
     * @return a new {@link Spy} instance for currenth thread only
     * @since 3.1
     */
    public static CurrentThreadSpy  spyCurrentThread() {
        return new CurrentThreadSpy();
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
     * and return the {@link Spy.SpyWithValue} object with stats
     * @param callable code to test
     * @param <V> type of return value
     * @return statistics on executed queries
     * @throws Exception if underlying code under test throws an Exception
     * @since 3.1
     */
    @SuppressWarnings("unchecked")
    public static <V> Spy.SpyWithValue<V> call(Callable<V> callable) throws Exception {
        return spy().call(callable);
    }
}
