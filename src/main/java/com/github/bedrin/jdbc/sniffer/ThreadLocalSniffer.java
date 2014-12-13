package com.github.bedrin.jdbc.sniffer;

/**
 * Created by bedrin on 13.12.2014.
 */
public class ThreadLocalSniffer extends ThreadLocal<Sniffer> {

    private final static ThreadLocalSniffer INSTANCE = new ThreadLocalSniffer();

    @Override
    protected Sniffer initialValue() {
        return new Sniffer();
    }

    public static void executeStatement() {
        INSTANCE.get().executeStatementImpl();
    }

    public static int executedStatements() {
        return INSTANCE.get().executedStatementsImpl();
    }

    public static void reset() {
        INSTANCE.get().resetImpl();
    }

    public static void verifyNotMore() {
        verifyNotMoreThan(0);
    }

    public static void verifyNotMoreThanOne() {
        verifyNotMoreThan(1);
    }

    public static void verifyNotMoreThan(int allowedStatements) throws IllegalStateException {
        int actualStatements = executedStatements();
        if (actualStatements > allowedStatements)
            throw new IllegalStateException(String.format("Allowed not more than %d statements, but actually caught %d statements", allowedStatements, actualStatements));
        reset();
    }

}
