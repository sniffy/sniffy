package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SpyWithValueTest extends BaseTest {

    @Test
    public void testVerifyNotMore() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyNever();
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatement();
            recordedQueries.verifyNever();
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOne() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce();
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce();
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatements(2);
            recordedQueries.verifyAtMostOnce();
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThan() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMost(2);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMost(2);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMost(2);
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatements(3);
            recordedQueries.verifyAtMost(2);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExact() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatement();
        recordedQueries.verify(1);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThan() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyAtLeast(2);
        recordedQueries.verifyAtLeast(1);
        try {
            recordedQueries.verifyAtLeast(3);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRange() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyBetween(2, 2);
        recordedQueries.verifyBetween(1, 2);
        recordedQueries.verifyBetween(2, 3);
        recordedQueries.verifyBetween(1, 3);
        try {
            recordedQueries.verifyBetween(3, 4);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyBetween(0, 1);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyNever(Threads.CURRENT);
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatementsInOtherThread(5);
            executeStatement();
            recordedQueries.verifyNever(Threads.CURRENT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce(Threads.CURRENT);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce(Threads.CURRENT);
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatements(2);
            recordedQueries.verifyAtMostOnce(Threads.CURRENT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatementsInOtherThread(3);
        recordedQueries.verifyAtMost(2, Threads.CURRENT);
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatements(3);
            recordedQueries.verifyAtMost(2, Threads.CURRENT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExactThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatement();
        recordedQueries.verify(1, Threads.CURRENT);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1, Threads.CURRENT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1, Threads.CURRENT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThanThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyAtLeast(2, Threads.CURRENT);
        recordedQueries.verifyAtLeast(1, Threads.CURRENT);
        try {
            recordedQueries.verifyAtLeast(3, Threads.CURRENT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRangeThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyBetween(2, 2, Threads.CURRENT);
        recordedQueries.verifyBetween(1, 2, Threads.CURRENT);
        recordedQueries.verifyBetween(2, 3, Threads.CURRENT);
        recordedQueries.verifyBetween(1, 3, Threads.CURRENT);
        try {
            recordedQueries.verifyBetween(3, 4, Threads.CURRENT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyBetween(0, 1, Threads.CURRENT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyNever(Threads.OTHERS);
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatementInOtherThread();
            recordedQueries.verifyNever(Threads.OTHERS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce(Threads.OTHERS);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce(Threads.OTHERS);
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatementsInOtherThread(2);
            recordedQueries.verifyAtMostOnce(Threads.OTHERS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMost(2, Threads.OTHERS);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMost(2, Threads.OTHERS);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMost(2, Threads.OTHERS);
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatementsInOtherThread(3);
            recordedQueries.verifyAtMost(2, Threads.OTHERS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExactOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatementInOtherThread();
        recordedQueries.verify(1, Threads.OTHERS);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1, Threads.OTHERS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1, Threads.OTHERS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThanOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatementsInOtherThread(2);
        recordedQueries.verifyAtLeast(2, Threads.OTHERS);
        recordedQueries.verifyAtLeast(1, Threads.OTHERS);
        try {
            recordedQueries.verifyAtLeast(3, Threads.OTHERS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRangeOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatementsInOtherThread(2);
        recordedQueries.verifyBetween(2, 2, Threads.OTHERS);
        recordedQueries.verifyBetween(1, 2, Threads.OTHERS);
        recordedQueries.verifyBetween(2, 3, Threads.OTHERS);
        recordedQueries.verifyBetween(1, 3, Threads.OTHERS);
        try {
            recordedQueries.verifyBetween(3, 4, Threads.OTHERS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyBetween(0, 1, Threads.OTHERS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }


}