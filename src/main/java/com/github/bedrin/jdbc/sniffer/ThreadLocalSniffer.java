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

    public static int executedStatements() {
        return getSniffer().executedStatementsImpl();
    }

}
