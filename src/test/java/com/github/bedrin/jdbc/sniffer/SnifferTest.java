package com.github.bedrin.jdbc.sniffer;

import junit.framework.Assert;
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
    public void testVerifyExact() throws Exception {
        // test positive
        Sniffer.reset();
        Sniffer.executeStatement();
        Sniffer.verifyExact(1);

        // test negative case 1
        try {
            Sniffer.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }

        // test negative case 2
        Sniffer.executeStatement();
        Sniffer.executeStatement();
        try {
            Sniffer.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesPositive() throws Exception {
        Sniffer.run(Sniffer::executeStatement).verifyNotMoreThanOne();
    }

    @Test
    public void testRecordQueriesNegative() throws Exception {
        try {
            Sniffer.run(Sniffer::executeStatement).verifyNoMoreQueries();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesThreadLocalPositive() throws Exception {
        Sniffer.execute(() -> {
            Sniffer.executeStatement();
            Thread thread = new Thread(Sniffer::executeStatement);
            thread.start();
            thread.join();
        }).verifyNotMoreThanOneThreadLocal();
    }

    @Test
    public void testRecordQueriesThreadLocalNegative() throws Exception {
        try {
            Sniffer.run(Sniffer::executeStatement).verifyNoMoreThreadLocalQueries();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesOtherThreadsPositive() throws Exception {
        Sniffer.execute(() -> {
            Sniffer.executeStatement();
            Thread thread = new Thread(Sniffer::executeStatement);
            thread.start();
            thread.join();
        }).verifyNotMoreThanOneOtherThreads();
    }

    @Test
    public void testRecordQueriesOtherThreadsNegative() throws Exception {
        try {
            Sniffer.execute(() -> {
                Sniffer.executeStatement();
                Thread thread = new Thread(Sniffer::executeStatement);
                thread.start();
                thread.join();
            }).verifyNoMoreOtherThreadsQueries();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesWithValue() throws Exception {
        assertEquals("test", Sniffer.call(() -> {
            Sniffer.executeStatement();
            return "test";
        }).verifyNotMoreThanOne().getValue());
    }

    @Test
    public void testTryWithResourceApi() throws Exception {
        try {
            try (ExpectedQueries ignored = Sniffer.expectNoMoreQueries()) {
                Sniffer.executeStatement();
                throw new RuntimeException("This is a test exception");
            }
        } catch (RuntimeException e) {
            assertEquals("This is a test exception", e.getMessage());
            assertNotNull(e.getSuppressed());
            assertEquals(1, e.getSuppressed().length);
            assertTrue(AssertionError.class.isAssignableFrom(e.getSuppressed()[0].getClass()));
        }
    }

}