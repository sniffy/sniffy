package com.github.bedrin.jdbc.sniffer;

/**
 * Sniffer holds the number of executed queries in current thread and provides static methods for accessing them
 * @since 1.0
 */
public class ThreadLocalSniffer extends ThreadLocal<Sniffer> {

    private final static ThreadLocalSniffer INSTANCE = new ThreadLocalSniffer();

    @Override
    protected Sniffer initialValue() {
        return new Sniffer();
    }

    static void executeStatement() {
        INSTANCE.get().executeStatementImpl();
    }

    /**
     * @return the number of executed queries in current thread since the last call of
     * {@link #reset() reset} method or to any of verify methods family like {@link #verifyNotMore() verifyNotMore},
     * {@link #verifyNotMoreThanOne() verifyNotMoreThanOne} or {@link #verifyNotMoreThan(int) verifyNotMoreThan}
     */
    public static int executedStatements() {
        return INSTANCE.get().executedStatementsImpl();
    }

    /**
     * Resets the queries counter to 0
     */
    public static void reset() {
        INSTANCE.get().resetImpl();
    }

    /**
     * Verifies that no queries has been executed in current thread since the last call of {@link #reset() reset} method
     * or to any of verify methods family
     * @throws IllegalStateException if actual number of executed statements exceeded 0
     */
    public static void verifyNotMore() {
        verifyNotMoreThan(0);
    }

    /**
     * Verifies that at most 1 query has been executed in current thread since the last call of {@link #reset() reset} method
     * or to any of verify methods family
     * @throws IllegalStateException if actual number of executed statements exceeded 1
     */
    public static void verifyNotMoreThanOne() {
        verifyNotMoreThan(1);
    }

    /**
     * Verifies that at most {@code allowedStatements} query has been executed  in current thread since the last call
     * of {@link #reset() reset} method or to any of verify methods family
     * @param allowedStatements maximum number of statements which could have been executed previously since
     *                          last {@link #reset() reset} call
     * @throws IllegalStateException if actual number of executed statements exceeded {@code allowedStatements}
     */
    public static void verifyNotMoreThan(int allowedStatements) throws IllegalStateException {
        int actualStatements = executedStatements();
        if (actualStatements > allowedStatements)
            throw new IllegalStateException(String.format("Allowed not more than %d statements, but actually caught %d statements", allowedStatements, actualStatements));
        reset();
    }

}
