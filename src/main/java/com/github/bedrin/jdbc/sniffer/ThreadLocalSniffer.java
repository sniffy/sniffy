package com.github.bedrin.jdbc.sniffer;

/**
 * Sniffer holds the number of executed queries in current thread and provides static methods for accessing them
 * @since 1.0
 */
public class ThreadLocalSniffer extends ThreadLocal<Sniffer> {

    private final static ThreadLocalSniffer INSTANCE = new ThreadLocalSniffer();

    @Override
    protected Sniffer initialValue() {
        return Sniffer.registerThreadLocalSniffer(new Sniffer());
    }

    static Sniffer getSniffer() {
        return INSTANCE.get();
    }
    
    static void executeStatement() {
        getSniffer().executeStatementImpl();
    }

    /**
     * @return the number of executed queries in current thread since the last call of
     * {@link #reset() reset} method or to any of verify methods family like {@link #verifyNotMore() verifyNotMore},
     * {@link #verifyNotMoreThanOne() verifyNotMoreThanOne} or {@link #verifyNotMoreThan(int) verifyNotMoreThan}
     * @since 1.0
     */
    public static int executedStatements() {
        return getSniffer().executedStatementsImpl();
    }

    /**
     * Resets the queries counter to 0
     * @since 1.0
     */
    @Deprecated
    public static void reset() {
        getSniffer().resetImpl();
    }

    /**
     * Verifies that no queries has been executed in current thread since the last call of {@link #reset() reset} method
     * or to any of verify methods family
     * @throws AssertionError if actual number of executed statements exceeded 0
     * @since 1.0
     */
    @Deprecated
    public static void verifyNotMore() {
        verifyNotMoreThan(0);
    }

    /**
     * Verifies that at most 1 query has been executed in current thread since the last call of {@link #reset() reset} method
     * or to any of verify methods family
     * @throws AssertionError if actual number of executed statements exceeded 1
     * @since 1.0
     */
    @Deprecated
    public static void verifyNotMoreThanOne() {
        verifyNotMoreThan(1);
    }

    /**
     * Verifies that at most {@code allowedStatements} queries has been executed in current thread since the last call
     * of {@link #reset() reset} method or to any of verify methods family
     * @param allowedStatements maximum number of statements which could have been executed previously since
     *                          last {@link #reset() reset} call
     * @throws AssertionError if actual number of executed statements exceeded {@code allowedStatements}
     * @since 1.0
     */
    @Deprecated
    public static void verifyNotMoreThan(int allowedStatements) throws AssertionError {
        verifyRange(0, allowedStatements);
    }

    /**
     * Verifies that exactly {@code allowedStatements} queries has been executed in current thread since the last call
     * of {@link #reset() reset} method or to any of verify methods family
     * @param allowedStatements number of statements which could have been executed previously since
     *                          last {@link #reset() reset} call
     * @throws AssertionError if illegal number of queries were performed
     * @since 1.3
     */
    @Deprecated
    public static void verifyExact(int allowedStatements) throws AssertionError {
        verifyRange(allowedStatements, allowedStatements);
    }

    /**
     * Verifies that at least {@code allowedStatements} queries has been executed in current thread since the last call
     * of {@link #reset() reset} method or to any of verify methods family
     * @param allowedStatements minimum number of statements which could have been executed previously since
     *                          last {@link #reset() reset} call
     * @throws AssertionError if illegal number of queries were performed
     * @since 1.4
     */
    @Deprecated
    public static void verifyNotLessThan(int allowedStatements) throws AssertionError {
        verifyRange(allowedStatements, Integer.MAX_VALUE);
    }

    /**
     * Verifies that at least {@code minAllowedStatements} queries and at most {@code maxAllowedStatements} has been
     * executed in current thread since the last call of {@link #reset() reset} method or to any of verify methods family
     * @param minAllowedStatements minimum number of statements which could have been executed previously since
     *                             last {@link #reset() reset} call
     * @param maxAllowedStatements maximum number of statements which could have been executed previously since
     *                             last {@link #reset() reset} call
     * @throws AssertionError if illegal number of queries were performed
     * @since 1.4
     */
    @Deprecated
    public static void verifyRange(int minAllowedStatements, int maxAllowedStatements) throws AssertionError {
        int actualStatements = executedStatements();
        if (actualStatements > maxAllowedStatements)
            throw new AssertionError(String.format("Allowed not more than %d statements, but actually caught %d statements", maxAllowedStatements, actualStatements));
        if (actualStatements < minAllowedStatements)
            throw new AssertionError(String.format("Allowed not less than %d statements, but actually caught %d statements", minAllowedStatements, actualStatements));
        reset();
    }

}
