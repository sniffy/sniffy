package io.sniffy;

import com.codahale.metrics.Timer;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.Protocol;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * Indicates if Sniffy potentially has global (non thread-local) spies registered
     * Once set to true it is never reseted to false
     *
     * Used for (premature) optimization only
     *
     * @since 3.1.9
     */
    private static volatile boolean hasGlobalSpies = false;

    /**
     * Indicates if Sniffy potentially has thread-local spies registered
     * Once set to true it is never reseted to false
     *
     * Used for (premature) optimization only
     *
     * @since 3.1.9
     */
    private static volatile boolean hasThreadLocalSpies = false;

    //@VisibleForTesting
    protected static final Queue<WeakReference<Spy>> registeredSpies =
            new ConcurrentLinkedQueue<WeakReference<Spy>>();
    //@VisibleForTesting
    protected static final ConcurrentMap<Long, WeakReference<CurrentThreadSpy>> currentThreadSpies =
            new ConcurrentHashMap<Long, WeakReference<CurrentThreadSpy>>();


    // TODO: add globalSocketStats
    protected static volatile ConcurrentLinkedHashMap<String, Timer> globalSqlStats =
            new ConcurrentLinkedHashMap.Builder<String, Timer>().
                    maximumWeightedCapacity(SniffyConfiguration.INSTANCE.getTopSqlCapacity()).
                    build();

    private static ThreadLocal<SocketStats> socketStatsAccumulator = new ThreadLocal<SocketStats>();

    public final static AtomicInteger CONNECTION_ID_SEQUENCE = new AtomicInteger();

    private static volatile boolean initialized = false;

    protected Sniffy() {
    }

    static {
        initialize();
    }

    @Deprecated
    public enum SniffyMode {
        DISABLED(false, false),
        ENABLED(true, true),
        ENABLED_NO_STACKTRACE(true, false);

        private final boolean enabled;
        private final boolean captureStackTraces;

        SniffyMode(boolean enabled, boolean captureStackTraces) {
            this.enabled = enabled;
            this.captureStackTraces = captureStackTraces;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isCaptureStackTraces() {
            return captureStackTraces;
        }
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

        if (SniffyConfiguration.INSTANCE.isMonitorNio()) {
            loadNioModule();
        } else {

            SniffyConfiguration.INSTANCE.addMonitorNioListener(new PropertyChangeListener() {

                private boolean sniffySelectorProviderInstalled = false;

                @Override
                public synchronized void propertyChange(PropertyChangeEvent evt) {
                    if (sniffySelectorProviderInstalled) return;
                    if (Boolean.TRUE.equals(evt.getNewValue())) {
                        loadNioModule();
                        sniffySelectorProviderInstalled = true;
                    }
                }

            });

        }

        initialized = true;

    }

    private static volatile boolean nioModuleLoaded = false;

    // TODO: do something more clever and extensible
    private static void loadNioModule() {
        if (!nioModuleLoaded) {
            synchronized (Sniffy.class) {
                if (!nioModuleLoaded) {

                    try {
                        Class.forName("io.sniffy.nio.SniffySelectorProviderModule").getMethod("initialize").invoke(null);
                        Class.forName("io.sniffy.nio.compat.SniffyCompatSelectorProviderModule").getMethod("initialize").invoke(null);;
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    nioModuleLoaded = true;

                }
            }
        }
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
        hasGlobalSpies = true;
        WeakReference<Spy> spyReference = new WeakReference<Spy>(spy);
        registeredSpies.add(spyReference);
        return spyReference;
    }

    protected static WeakReference<CurrentThreadSpy> registerCurrentThreadSpy(CurrentThreadSpy spy) {
        hasThreadLocalSpies = true;
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

        if (hasGlobalSpies) {
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

        if (hasThreadLocalSpies) {
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

    }

    private static void notifyListeners(StatementMetaData statementMetaData) {

        if (hasGlobalSpies) {
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

        if (hasThreadLocalSpies) {
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

    }

    private static void notifyListeners(SocketMetaData socketMetaData, long elapsedTime, int bytesDown, int bytesUp) {

        if (hasGlobalSpies) {
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

        if (hasThreadLocalSpies) {
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
    }

    private static void notifyListeners(SocketMetaData socketMetaData, boolean sent, long timestamp, byte[] traffic, int off, int len) {

        if (hasGlobalSpies) {
            Iterator<WeakReference<Spy>> iterator = registeredSpies.iterator();
            while (iterator.hasNext()) {
                WeakReference<Spy> spyReference = iterator.next();
                Spy spy = spyReference.get();
                if (null == spy) {
                    iterator.remove();
                } else {
                    spy.addNetworkTraffic(socketMetaData, sent, timestamp, traffic, off, len);
                }
            }
        }

        if (hasThreadLocalSpies) {
            Long threadId = Thread.currentThread().getId();

            WeakReference<CurrentThreadSpy> spyReference = currentThreadSpies.get(threadId);
            if (null != spyReference) {
                CurrentThreadSpy spy = spyReference.get();
                if (null == spy) {
                    currentThreadSpies.remove(threadId);
                } else {
                    spy.addNetworkTraffic(socketMetaData, sent, timestamp, traffic, off, len);
                }
            }
        }
    }

    // TODO: use getEffectiveSpyConfiguration() instead
    @Deprecated
    public static boolean hasSpies() {
        return getSniffyMode().isEnabled();
    }

    /**
     * @since 3.1.10
     */
    public static SpyConfiguration getEffectiveSpyConfiguration() {

        SpyConfiguration.Builder builder = SpyConfiguration.builder().
                captureJdbc(false).
                captureNetwork(false).
                captureNetworkTraffic(false).
                captureStackTraces(false);

        if (hasGlobalSpies) {
            if (!registeredSpies.isEmpty()) {
                Iterator<WeakReference<Spy>> iterator = registeredSpies.iterator();
                while (iterator.hasNext()) {
                    WeakReference<Spy> spyReference = iterator.next();
                    Spy spy = spyReference.get();
                    if (null == spy) {
                        iterator.remove();
                    } else {
                        builder = builder.or(spy.getSpyConfiguration());
                    }
                }
            }
        }

        if (hasThreadLocalSpies) {
            Long threadId = Thread.currentThread().getId();

            WeakReference<CurrentThreadSpy> spyReference = currentThreadSpies.get(threadId);
            if (null != spyReference) {
                CurrentThreadSpy spy = spyReference.get();
                if (null == spy) {
                    currentThreadSpies.remove(threadId);
                } else {
                    builder = builder.or(spy.getSpyConfiguration());
                }
            }
        }

        return builder.build();

    }

    // TODO: use getEffectiveSpyConfiguration() instead
    /**
     * @since 3.1.6
     */
    @Deprecated
    public static SniffyMode getSniffyMode() {

        SpyConfiguration effectiveSpyConfiguration = getEffectiveSpyConfiguration();

        if (effectiveSpyConfiguration.isCaptureJdbc() || effectiveSpyConfiguration.isCaptureNetwork()) {
            return effectiveSpyConfiguration.isCaptureStackTraces() ? SniffyMode.ENABLED : SniffyMode.ENABLED_NO_STACKTRACE;
        } else {
            return SniffyMode.DISABLED;
        }

    }

    // TODO: merge with logTraffic
    public static void logSocket(int connectionId, InetSocketAddress address, long elapsedTime, int bytesDown, int bytesUp) {
        logSocket(connectionId, address, elapsedTime, bytesDown, bytesUp, true);
    }

    // TODO: merge with logTraffic
    public static void logSocket(int connectionId, InetSocketAddress address, long elapsedTime, int bytesDown, int bytesUp, boolean captureStackTraces) {

        // do not track JDBC socket operations
        SocketStats socketStats = socketStatsAccumulator.get();
        if (null != socketStats) {
            socketStats.accumulate(elapsedTime, bytesDown, bytesUp);
        } else {
            // build stackTrace
            String stackTrace = captureStackTraces ? printStackTrace(getTraceTillPackage("java.net")) : null; // TODO: is stacktrace different for NIO and NIO2 ?

            // increment counters
            SocketMetaData socketMetaData = new SocketMetaData(address, connectionId, stackTrace, Thread.currentThread());

            // notify listeners
            notifyListeners(socketMetaData, elapsedTime, bytesDown, bytesUp);
        }
    }

    public static void logTraffic(int connectionId, InetSocketAddress address, boolean sent, Protocol protocol, byte[] traffic, int off, int len, boolean captureStackTraces) {

        // build stackTrace
        String stackTrace = captureStackTraces ? printStackTrace(getTraceTillPackage("java.net")) : null;

        SocketMetaData socketMetaData = new SocketMetaData(protocol, address, connectionId, stackTrace, Thread.currentThread());

        // notify listeners
        notifyListeners(socketMetaData, sent, System.currentTimeMillis(), traffic, off, len);

    }

    public static void enterJdbcMethod() {
        // TODO: check if it supports reentrant calls
        socketStatsAccumulator.set(new SocketStats(0, 0, 0));
    }

    public static void exitJdbcMethod(Method method, long elapsedTime) {
        exitJdbcMethod(method, elapsedTime, null);
    }

    public static void exitJdbcMethod(Method method, long elapsedTime, Method implMethod) {

        SniffyMode sniffyMode = Sniffy.getSniffyMode();

        if (sniffyMode.isEnabled()) {
            // get accumulated socket stats
            SocketStats socketStats = socketStatsAccumulator.get();

            if (null != socketStats) {

                if (socketStats.bytesDown.longValue() > 0 || socketStats.bytesUp.longValue() > 0) {
                    String stackTrace = null;
                    if (sniffyMode.isCaptureStackTraces()) {
                        try {
                            stackTrace = printStackTrace(null == implMethod ?
                                    getTraceForProxiedMethod(method) :
                                    getTraceForImplementingMethod(method, implMethod)
                            );
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    StatementMetaData statementMetaData = new StatementMetaData(
                            method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()",
                            SqlStatement.SYSTEM,
                            stackTrace,
                            Thread.currentThread()
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
        StatementMetaData statementMetaData = new StatementMetaData(sql, SqlUtil.guessQueryType(sql), stackTrace, Thread.currentThread());
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
     * @return a new {@link Spy} instance with given configuration
     * @since 3.1.10
     */
    public static <T extends Spy<T>> Spy<? extends Spy<T>> spy(SpyConfiguration spyConfiguration) {
        return new Spy<T>(spyConfiguration);
    }

    /**
     * @return a new {@link Spy} instance for current thread only
     * @since 3.1
     */
    public static CurrentThreadSpy spyCurrentThread() {
        return spyCurrentThread(true);
    }

    /**
     * @return a new {@link Spy} instance for current thread only
     * @since 3.1
     */
    public static CurrentThreadSpy spyCurrentThread(boolean captureStackTraces) {
        return new CurrentThreadSpy(captureStackTraces);
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
