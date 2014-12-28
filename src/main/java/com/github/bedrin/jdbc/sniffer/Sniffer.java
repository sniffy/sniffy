package com.github.bedrin.jdbc.sniffer;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sniffer holds the number of executed queries and provides static methods for accessing them
 * @since 1.0
 */
public class Sniffer {

    private final AtomicInteger counter = new AtomicInteger();

    private final static Sniffer INSTANCE = new Sniffer();

    int executedStatementsImpl() {
        return counter.get();
    }

    void resetImpl() {
        counter.set(0);
    }

    int executeStatementImpl() {
        return counter.incrementAndGet();
    }

    final static Set<Sniffer> threadLocalSniffers = Collections.newSetFromMap(
            new ConcurrentHashMap<Sniffer, Boolean>()
    );

    static Sniffer registerThreadLocalSniffer(Sniffer tlSniffer) {
        threadLocalSniffers.add(tlSniffer);
        return tlSniffer;
    }

    static Collection<Sniffer> getThreadLocalSniffers() {
        return threadLocalSniffers;
    }

    static void executeStatement() {
        INSTANCE.executeStatementImpl();
        ThreadLocalSniffer.executeStatement();
    }

    /**
     * @return the number of executed queries since the last call of {@link #reset() reset} method or to any of verify
     * methods family like {@link #verifyNotMore() verifyNotMore}, {@link #verifyNotMoreThanOne() verifyNotMoreThanOne}
     * or {@link #verifyNotMoreThan(int) verifyNotMoreThan}
     */
    public static int executedStatements() {
        return INSTANCE.executedStatementsImpl();
    }

    /**
     * Resets the queries counter to 0
     */
    public static void reset() {
        INSTANCE.resetImpl();
    }

    /**
     * Verifies that no queries has been executed since the last call of {@link #reset() reset} method or to any of verify
     * methods family
     * @throws IllegalStateException if actual number of executed statements exceeded 0
     */
    public static void verifyNotMore() {
        verifyNotMoreThan(0);
    }

    /**
     * Verifies that at most 1 query has been executed since the last call of {@link #reset() reset} method or to any of verify
     * methods family
     * @throws IllegalStateException if actual number of executed statements exceeded 1
     */
    public static void verifyNotMoreThanOne() {
        verifyNotMoreThan(1);
    }

    /**
     * Verifies that at most {@code allowedStatements} query has been executed since the last call of
     * {@link #reset() reset} method or to any of verify methods family
     * @param allowedStatements maximum number of statements which could have been executed previously since
     *                          last {@link #reset() resetC} call
     * @throws IllegalStateException if actual number of executed statements exceeded {@code allowedStatements}
     */
    public static void verifyNotMoreThan(int allowedStatements) throws IllegalStateException {
        verifyRange(0, allowedStatements);
    }

    /**
     *
     * @param allowedStatements
     * @throws IllegalStateException
     * @since 1.3
     */
    public static void verifyExact(int allowedStatements) throws IllegalStateException {
        verifyRange(allowedStatements, allowedStatements);
    }

    /**
     *
     * @param allowedStatements
     * @throws IllegalStateException
     * @since 1.3
     */
    public static void verifyNotLessThan(int allowedStatements) throws IllegalStateException {
        verifyRange(allowedStatements, Integer.MAX_VALUE);
    }

    /**
     *
     * @param minAllowedStatements
     * @param maxAllowedStatements
     * @throws IllegalStateException
     * @since 1.3
     */
    public static void verifyRange(int minAllowedStatements, int maxAllowedStatements) throws IllegalStateException {
        int actualStatements = executedStatements();
        if (actualStatements > maxAllowedStatements)
            throw new IllegalStateException(String.format("Allowed not more than %d statements, but actually caught %d statements", maxAllowedStatements, actualStatements));
        if (actualStatements < minAllowedStatements)
            throw new IllegalStateException(String.format("Allowed not less than %d statements, but actually caught %d statements", minAllowedStatements, actualStatements));
        reset();
    }

    public static RecordedQueries recordQueries(Runnable runnable) {
        int queries = executedStatements();
        runnable.run();
        return new RecordedQueries(executedStatements() - queries);
    }

    public static class RecordedQueries {

        private final int executedStatements;

        public RecordedQueries(int executedStatements) {
            this.executedStatements = executedStatements;
        }

        public void verifyNotMore() {
            verifyNotMoreThan(0);
        }

        public void verifyNotMoreThanOne() {
            verifyNotMoreThan(1);
        }

        public void verifyNotMoreThan(int allowedStatements) throws IllegalStateException {
            if (executedStatements > allowedStatements)
                throw new IllegalStateException(String.format("Allowed not more than %d statements, but actually caught %d statements", allowedStatements, executedStatements));
        }

    }

}
