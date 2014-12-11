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

}
