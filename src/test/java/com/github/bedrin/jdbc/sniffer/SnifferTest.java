package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class SnifferTest extends BaseTest {

    @Test
    public void testExecuteStatement() throws Exception {
        int actual = Sniffer.executedStatements();
        executeStatement();
        assertEquals(1, Sniffer.executedStatements() - actual);
    }

    @Test
    public void testVerifyExact() throws Exception {
        // test positive
        Spy spy = Sniffer.spy();
        executeStatement();
        spy.verify(1);

        // test negative case 1
        spy = Sniffer.spy();
        try {
            spy.verify(1);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }

        // test negative case 2
        spy = Sniffer.spy();
        executeStatements(2);
        try {
            spy.verify(1);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesPositive() throws Exception {
        Sniffer.run(BaseTest::executeStatement).verifyAtMostOnce();
    }

    @Test
    public void testRecordQueriesNegative() throws Exception {
        try {
            Sniffer.run(BaseTest::executeStatement).verifyNever();
            fail();
        } catch (WrongNumberOfQueriesError e) {
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
        }).verifyAtMostOnce(Sniffer.CURRENT_THREAD);
    }

    @Test
    public void testRecordQueriesThreadLocalNegative() throws Exception {
        try {
            Sniffer.run(BaseTest::executeStatement).verifyNever(Sniffer.CURRENT_THREAD);
            fail();
        } catch (WrongNumberOfQueriesError e) {
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
        }).verifyAtMostOnce(Sniffer.OTHER_THREADS);
    }

    @Test
    public void testRecordQueriesOtherThreadsNegative() throws Exception {
        try {
            Sniffer.execute(() -> {
                executeStatement();
                Thread thread = new Thread(BaseTest::executeStatement);
                thread.start();
                thread.join();
            }).verifyNever(Sniffer.OTHER_THREADS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesWithValue() throws Exception {
        assertEquals("test", Sniffer.call(() -> {
            executeStatement();
            return "test";
        }).verifyAtMostOnce().getValue());
    }

    @Test
    public void testTryWithResourceApi() throws Exception {
        try {
            try (Spy ignored = Sniffer.expectNever()) {
                executeStatement();
                throw new RuntimeException("This is a test exception");
            }
        } catch (RuntimeException e) {
            assertEquals("This is a test exception", e.getMessage());
            assertNotNull(e.getSuppressed());
            assertEquals(1, e.getSuppressed().length);
            assertTrue(WrongNumberOfQueriesError.class.isAssignableFrom(e.getSuppressed()[0].getClass()));
        }
    }

    @Test
    public void testExpectNotMoreThanOne() {
        // positive
        try (Spy ignored = Sniffer.expectAtMostOnce()) {
            executeStatement();
        }
        // negative
        try {
            try (Spy ignored = Sniffer.expectAtMostOnce()) {
                executeStatements(2);
            }
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        // positive thread local
        try (Spy ignored = Sniffer.expectAtMostOnce(Sniffer.CURRENT_THREAD)) {
            executeStatement();
            executeStatementInOtherThread();
        }
    }

}