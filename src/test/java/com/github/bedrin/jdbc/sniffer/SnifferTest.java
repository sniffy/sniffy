package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class SnifferTest extends BaseTest {

    @Test
    public void testResetImpl() throws Exception {
        Sniffer.reset();
        assertEquals(0, Sniffer.executedStatements());
        executeStatement();
        Sniffer.reset();
        assertEquals(0, Sniffer.executedStatements());
    }

    @Test
    public void testExecuteStatement() throws Exception {
        Sniffer.reset();
        assertEquals(0, Sniffer.executedStatements());
        executeStatement();
        assertEquals(1, Sniffer.executedStatements());
    }

    @Test
    public void testVerifyExact() throws Exception {
        // test positive
        Sniffer.reset();
        executeStatement();
        Sniffer.verifyExact(1);

        // test negative case 1
        try {
            Sniffer.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }

        // test negative case 2
        executeStatements(2);
        try {
            Sniffer.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesPositive() throws Exception {
        Sniffer.run(BaseTest::executeStatement).verifyNotMoreThanOne();
    }

    @Test
    public void testRecordQueriesNegative() throws Exception {
        try {
            Sniffer.run(BaseTest::executeStatement).verifyNoMore();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesThreadLocalPositive() throws Exception {
        Sniffer.execute(() -> {
            executeStatement();
            Thread thread = new Thread(BaseTest::executeStatement);
            thread.start();
            thread.join();
        }).verifyNotMoreThanOne(Sniffer.CURRENT_THREAD);
    }

    @Test
    public void testRecordQueriesThreadLocalNegative() throws Exception {
        try {
            Sniffer.run(BaseTest::executeStatement).verifyNoMore(Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesOtherThreadsPositive() throws Exception {
        Sniffer.execute(() -> {
            executeStatement();
            Thread thread = new Thread(BaseTest::executeStatement);
            thread.start();
            thread.join();
        }).verifyNotMoreThanOne(Sniffer.OTHER_THREADS);
    }

    @Test
    public void testRecordQueriesOtherThreadsNegative() throws Exception {
        try {
            Sniffer.execute(() -> {
                executeStatement();
                Thread thread = new Thread(BaseTest::executeStatement);
                thread.start();
                thread.join();
            }).verifyNoMore(Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesWithValue() throws Exception {
        assertEquals("test", Sniffer.call(() -> {
            executeStatement();
            return "test";
        }).verifyNotMoreThanOne().getValue());
    }

    @Test
    public void testTryWithResourceApi() throws Exception {
        try {
            try (ExpectedQueries ignored = Sniffer.expectNoMore()) {
                executeStatement();
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