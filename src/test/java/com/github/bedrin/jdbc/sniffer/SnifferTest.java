package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class SnifferTest {

    @Test
    public void testResetImpl() throws Exception {
        Sniffer.reset();
        assertEquals(0, Sniffer.executedStatements());
        Sniffer.executeStatement();
        Sniffer.reset();
        assertEquals(0, Sniffer.executedStatements());
    }

    @Test
    public void testExecuteStatement() throws Exception {
        Sniffer.reset();
        assertEquals(0, Sniffer.executedStatements());
        Sniffer.executeStatement();
        assertEquals(1, Sniffer.executedStatements());
    }

    @Test
    public void testRecordQueriesPositive() throws Exception {
        Sniffer.recordQueries(Sniffer::executeStatement).verifyNotMoreThanOne();
    }

    @Test
    public void testRecordQueriesNegative() throws Exception {
        try {
            Sniffer.recordQueries(Sniffer::executeStatement).verifyNotMore();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesThreadLocalPositive() throws Exception {
        Sniffer.recordQueries(() -> {
            Sniffer.executeStatement();
            Thread thread = new Thread(Sniffer::executeStatement);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }).verifyNotMoreThanOneThreadLocal();
    }

    @Test
    public void testRecordQueriesThreadLocalNegative() throws Exception {
        try {
            Sniffer.recordQueries(Sniffer::executeStatement).verifyNotMoreThreadLocal();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesOtherThreadsPositive() throws Exception {
        Sniffer.recordQueries(() -> {
            Sniffer.executeStatement();
            Thread thread = new Thread(Sniffer::executeStatement);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }).verifyNotMoreThanOneOtherThreads();
    }

    @Test
    public void testRecordQueriesOtherThreadsNegative() throws Exception {
        try {
            Sniffer.recordQueries(() -> {
                Sniffer.executeStatement();
                Thread thread = new Thread(Sniffer::executeStatement);
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            }).verifyNotMoreOtherThreads();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

}