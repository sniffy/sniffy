package io.sniffy;

import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.SocketUtil;
import io.sniffy.util.StringUtil;

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
 * @see Sniffy#spy()
 * @see Sniffy#expect(Expectation)
 * @since 2.0
 */
public class Spy<C extends Spy<C>> extends LegacySpy<C> implements Closeable {

    private final WeakReference<Spy> selfReference;
    private final long ownerThreadId;

    private boolean closed = false;
    private StackTraceElement[] closeStackTrace;

    private List<Expectation> expectations = new ArrayList<Expectation>();

    /**
     * @since 3.1
     */
    @Override
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

    Spy() {
        ownerThreadId = Thread.currentThread().getId();
        selfReference = Sniffy.registerSpy(this);
    }

    /**
     * Wrapper for {@link Sniffy#spy()} method; useful for chaining
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    public C reset() {
        checkOpened();
        super.reset();
        expectations.clear();
        return self();
    }

    /**
     * @since 3.1
     */
    public Map<SocketMetaData, SocketStats> getSocketOperations(Threads threadMatcher, String address, boolean removeStackTraces) {

        Map.Entry<String, Integer> hostAndPort = SocketUtil.parseSocketAddress(address);

        String hostName = hostAndPort.getKey();
        Integer port = hostAndPort.getValue();

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
                    (null == hostName || hostName.equals(inetAddress.getHostName()) || hostName.equals(inetAddress.getHostAddress())) &&
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

    // Expect and verify methods


    /**
     * @param expectation
     * @return
     * @since 3.1
     */
    @Override
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
    @Override
    public C verify(Expectation expectation) {
        checkOpened();
        expectation.verify(this);
        return self();
    }

    /**
     * Verifies all expectations added previously using {@code expect} methods family
     * @throws SniffyAssertionError if wrong number of queries was executed
     * @since 2.0
     */
    @Override
    public void verify() throws SniffyAssertionError {
        checkOpened();
        SniffyAssertionError assertionError = getSniffyAssertionError();
        if (null != assertionError) {
            throw assertionError;
        }
    }

    /**
     * @return SniffyAssertionError or null if there are no errors
     * @since 3.1
     */
    @Override
    public SniffyAssertionError getSniffyAssertionError() {
        checkOpened();
        SniffyAssertionError assertionError = null;
        Throwable currentException = null;
        for (Expectation expectation : expectations) {
            try {
                expectation.verify(this);
            } catch (SniffyAssertionError e) {
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
            Sniffy.removeSpyReference(selfReference);
            closed = true;
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            closeStackTrace = new StackTraceElement[stackTrace.length - 1];
            System.arraycopy(stackTrace, 1, closeStackTrace, 0, stackTrace.length - 1);
        }
    }

    @Override
    protected void checkOpened() {
        if (closed) {
            throw new SpyClosedException("Spy is closed", closeStackTrace);
        }
    }

    /**
     * Executes the {@link io.sniffy.Executable#execute()} method on provided argument and verifies the expectations
     * @throws SniffyAssertionError if wrong number of queries was executed
     * @since 3.1
     */
    @Override
    public C execute(io.sniffy.Executable executable) throws SniffyAssertionError {
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
     * @throws SniffyAssertionError if wrong number of queries was executed
     * @since 2.0
     */
    public C run(Runnable runnable) throws SniffyAssertionError {
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
     * @throws SniffyAssertionError if wrong number of queries was executed
     * @since 2.0
     */
    public <V> SpyWithValue<V> call(Callable<V> callable) throws SniffyAssertionError {
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
        } catch (SniffyAssertionError ae) {
            if (!ExceptionUtil.addSuppressed(e, ae)) {
                ae.printStackTrace();
            }
        }
        throwException(e);
        return new RuntimeException(e);
    }

    /**
     * @since 3.1
     */
    public interface Expectation {

        <T extends Spy<T>> Spy<T> verify(Spy<T> spy) throws SniffyAssertionError;

    }

    /**
     * @since 3.1
     */
    public static class SpyClosedException extends IllegalStateException {

        private final StackTraceElement[] closeStackTrace;

        public SpyClosedException(String s, StackTraceElement[] closeStackTrace) {
            super(ExceptionUtil.generateMessage(s + StringUtil.LINE_SEPARATOR + "Close stack trace:", closeStackTrace));
            this.closeStackTrace = closeStackTrace;
        }

        public StackTraceElement[] getCloseStackTrace() {
            return null == closeStackTrace ? null : closeStackTrace.clone();
        }

    }

    public static final class SpyWithValue<V> extends Spy<SpyWithValue<V>> {

        private final V value;

        SpyWithValue(V value) {
            this.value = value;
        }

        public V getValue() {
            return value;
        }

    }
}
