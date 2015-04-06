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
        return executedStatements(true);
    }

    public static int executedStatements(boolean sinceLastReset) {
        return getSniffer().executedStatementsImpl(sinceLastReset);
    }

}
