package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

public class SnifferExpectApiTest {

    @Test(expected = AssertionError.class)
    public void testNotMoreThan() throws Exception {
        try (ExpectedQueries eq = Sniffer.expectNotMoreThan(1)) {
            Sniffer.executeStatement();
            Sniffer.executeStatement();
        }
    }

    @Test
    public void testNotMoreThanAllThreads() throws Exception {
        try (ExpectedQueries eq = Sniffer.expectNotMoreThanThreadLocal(1)) {
            Thread thread = new Thread(Sniffer::executeStatement);
            thread.start();
            thread.join();
            Sniffer.executeStatement();
        }
    }

    @Test
    public void testNotMoreThanOtherThreads() throws Exception {
        try (ExpectedQueries eq = Sniffer.expectNotMoreThanOtherThreads(1)) {
            Thread thread = new Thread(Sniffer::executeStatement);
            thread.start();
            thread.join();
            Sniffer.executeStatement();
            Sniffer.executeStatement();
        }
    }

}