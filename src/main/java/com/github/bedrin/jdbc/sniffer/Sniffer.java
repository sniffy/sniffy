package com.github.bedrin.jdbc.sniffer;

import java.util.concurrent.atomic.AtomicInteger;

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

    static void executeStatement() {
        INSTANCE.executeStatementImpl();
        ThreadLocalSniffer.executeStatement();
    }

    public static int executedStatements() {
        return INSTANCE.executedStatementsImpl();
    }

    public static void reset() {
        INSTANCE.resetImpl();
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
