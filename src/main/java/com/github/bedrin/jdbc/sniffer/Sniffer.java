package com.github.bedrin.jdbc.sniffer;

import java.util.concurrent.atomic.AtomicInteger;

public class Sniffer {

    private static final AtomicInteger counter = new AtomicInteger();

    public static void executeStatement() {
        counter.incrementAndGet();
    }

    public static int executedStatements() {
        return counter.get();
    }

    public static void reset() {
        counter.set(0);
    }
    
    public static void verifyNotMore() {
        verifyNotMoreThan(0);
    }

    public static void verifyNotMoreThanOne() {
        verifyNotMoreThan(1);
    }

    public static void verifyNotMoreThan(int allowedStatements) throws IllegalStateException {
        if (executedStatements() > allowedStatements)
            throw new IllegalStateException(String.format("Allowed not more than %d statements, but actually caught %d statements", allowedStatements, actualStatements));
        reset();
    }    

}
