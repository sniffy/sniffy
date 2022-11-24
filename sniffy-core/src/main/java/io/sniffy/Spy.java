package io.sniffy;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.reflection.Unsafe;
import io.sniffy.socket.*;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.StringUtil;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Callable;

import static io.sniffy.reflection.Unsafe.throwException;

/**
 * Spy holds a number of queries which were executed at some point of time and uses it as a base for further assertions
 *
 * @see Sniffy#spy()
 * @see Sniffy#expect(Expectation)
 * @since 2.0
 */
public class Spy<C extends Spy<C>> extends LegacySpy<C> implements Closeable {

    private final WeakReference<Spy> selfReference;

    private boolean closed = false;
    private StackTraceElement[] closeStackTrace;

    private List<Expectation> expectations = new ArrayList<Expectation>();

    /**
     * @since 3.1
     */
    @Override
    public Map<StatementMetaData, SqlStats> getExecutedStatements(ThreadMatcher threadMatcher, boolean removeStackTraces) {

        Map<StatementMetaData, SqlStats> executedStatements = new LinkedHashMap<StatementMetaData, SqlStats>();
        for (Map.Entry<StatementMetaData, SqlStats> entry : this.executedStatements.ascendingMap().entrySet()) {

            StatementMetaData statementMetaData = entry.getKey();

            if (removeStackTraces) statementMetaData = new StatementMetaData(
                    statementMetaData.sql, statementMetaData.query, null, statementMetaData.getThreadMetaData()
            );

            if (threadMatcher.matches(statementMetaData.getThreadMetaData())) {
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
        this(SpyConfiguration.builder().build());
    }

    Spy(SpyConfiguration spyConfiguration) {
        super(spyConfiguration);
        selfReference = Sniffy.registerSpy(this);
    }

    /**
     * Wrapper for {@link Sniffy#spy()} method; useful for chaining
     *
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
    public Map<SocketMetaData, SocketStats> getSocketOperations(ThreadMatcher threadMatcher, String address, boolean removeStackTraces) {
        return getSocketOperations(threadMatcher, AddressMatchers.exactAddressMatcher(address), removeStackTraces);
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, SocketStats> getSocketOperations(ThreadMatcher threadMatcher, boolean removeStackTraces) {
        return getSocketOperations(threadMatcher, AddressMatchers.anyAddressMatcher(), removeStackTraces);
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, SocketStats> getSocketOperations(ThreadMatcher threadMatcher, AddressMatcher addressMatcher, boolean removeStackTraces) {

        Map<SocketMetaData, SocketStats> socketOperations = new LinkedHashMap<SocketMetaData, SocketStats>();
        for (Map.Entry<SocketMetaData, SocketStats> entry : this.socketOperations.ascendingMap().entrySet()) {
            SocketMetaData socketMetaData = entry.getKey();
            if (threadMatcher.matches(socketMetaData.getThreadMetaData()) && (null == addressMatcher || addressMatcher.matches(socketMetaData.getAddress()))) {
                if (removeStackTraces) socketMetaData = new SocketMetaData(
                        socketMetaData.getProtocol(), socketMetaData.address, socketMetaData.connectionId, null, socketMetaData.getThreadMetaData()
                );
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
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic() {
        return getNetworkTraffic(Threads.ANY, AddressMatchers.anyAddressMatcher());
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic(ThreadMatcher threadMatcher, String address) {
        return getNetworkTraffic(threadMatcher, AddressMatchers.exactAddressMatcher(address));
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic(ThreadMatcher threadMatcher, String address, GroupingOptions groupingOptions) {
        return getNetworkTraffic(threadMatcher, AddressMatchers.exactAddressMatcher(address), groupingOptions);
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic(ThreadMatcher threadMatcher, AddressMatcher addressMatcher) {
        return getNetworkTraffic(threadMatcher, addressMatcher, GroupingOptions.builder().build());
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic(ThreadMatcher threadMatcher, AddressMatcher addressMatcher, GroupingOptions groupingOptions) {
        return filterTraffic(this.networkTraffic, threadMatcher, addressMatcher, groupingOptions);
    }

    /**
     * @since 3.1.11
     */
    public Map<SocketMetaData, List<NetworkPacket>> getDecryptedNetworkTraffic(ThreadMatcher threadMatcher, AddressMatcher addressMatcher, GroupingOptions groupingOptions) {
        return filterTraffic(this.decryptedNetworkTraffic, threadMatcher, addressMatcher, groupingOptions);
    }

    private Map<SocketMetaData, List<NetworkPacket>> filterTraffic(ConcurrentLinkedHashMap<SocketMetaData, Deque<NetworkPacket>> originalTraffic, ThreadMatcher threadMatcher, AddressMatcher addressMatcher, GroupingOptions groupingOptions) {
        Map<SocketMetaData, List<NetworkPacket>> reducedTraffic = new LinkedHashMap<SocketMetaData, List<NetworkPacket>>();

        for (Map.Entry<SocketMetaData, Deque<NetworkPacket>> entry : originalTraffic.ascendingMap().entrySet()) {

            SocketMetaData socketMetaData = entry.getKey();
            Deque<NetworkPacket> networkPackets = entry.getValue();

            if (addressMatcher.matches(socketMetaData.getAddress()) &&
                    (null == socketMetaData.getThreadMetaData() || threadMatcher.matches(socketMetaData.getThreadMetaData()))) {

                if (Unsafe.getJavaVersion() < 7) {
                    // TODO: backport ConcurrentLinkedDeque for Java 1.6 and remove this code
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (networkPackets) {
                        filterNetworkPackets(threadMatcher, groupingOptions, reducedTraffic, socketMetaData, networkPackets);
                    }
                } else {
                    filterNetworkPackets(threadMatcher, groupingOptions, reducedTraffic, socketMetaData, networkPackets);
                }

            }

        }

        return reducedTraffic;
    }

    private void filterNetworkPackets(ThreadMatcher threadMatcher, GroupingOptions groupingOptions, Map<SocketMetaData, List<NetworkPacket>> reducedTraffic, SocketMetaData socketMetaData, Deque<NetworkPacket> networkPackets) {
        for (NetworkPacket networkPacket : networkPackets) {

            if (threadMatcher.matches(networkPacket.getThreadMetaData())) {

                SocketMetaData reducedSocketMetaData = new SocketMetaData(
                        socketMetaData.getProtocol(),
                        socketMetaData.getAddress(),
                        groupingOptions.isGroupByConnection() ? socketMetaData.getConnectionId() : -1,
                        groupingOptions.isGroupByStackTrace() ? networkPacket.getStackTrace() : null,
                        groupingOptions.isGroupByThread() ? networkPacket.getThreadMetaData() : null
                );

                List<NetworkPacket> reducedNetworkPackets = reducedTraffic.get(reducedSocketMetaData);
                //noinspection Java8MapApi
                if (null == reducedNetworkPackets) {
                    reducedNetworkPackets = new ArrayList<NetworkPacket>();
                    reducedTraffic.put(reducedSocketMetaData, reducedNetworkPackets);
                }

                if ((getSpyConfiguration().isCaptureStackTraces() && !groupingOptions.isGroupByStackTrace())
                        || !groupingOptions.isGroupByThread()) {
                    byte[] bytes = networkPacket.getBytes();
                    networkPacket = new NetworkPacket(
                            networkPacket.isSent(),
                            networkPacket.getTimestamp(),
                            groupingOptions.isGroupByStackTrace() ? networkPacket.getStackTrace() : null,
                            groupingOptions.isGroupByThread() ? networkPacket.getThreadMetaData() : null,
                            bytes, 0, bytes.length
                    );
                }

                if (reducedNetworkPackets.isEmpty()) {
                    reducedNetworkPackets.add(networkPacket);
                } else {
                    NetworkPacket lastPacket = reducedNetworkPackets.get(reducedNetworkPackets.size() - 1);
                    if (!lastPacket.combine(networkPacket, SniffyConfiguration.INSTANCE.getPacketMergeThreshold())) {
                        reducedNetworkPackets.add(networkPacket);
                    }
                }

            }

        }
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
     *
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
     *
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
     *
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
     *
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
     *
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
