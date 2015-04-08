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
            recordedQueries.verifyNever();
            fail();
        } catch (AssertionError e) {
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
            recordedQueries.verifyAtMostOnce();
            fail();
        } catch (AssertionError e) {
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
            recordedQueries.verifyAtMost(2);
            fail();
        } catch (AssertionError e) {
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
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1);
            fail();
        } catch (AssertionError e) {
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
        } catch (AssertionError e) {
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
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyBetween(0, 1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyNever(Sniffer.CURRENT_THREAD);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verifyNever(Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce(Sniffer.CURRENT_THREAD);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce(Sniffer.CURRENT_THREAD);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verifyAtMostOnce(Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatementsInOtherThread(3);
        recordedQueries.verifyAtMost(2, Sniffer.CURRENT_THREAD);
        recordedQueries = new SpyWithValue<>("val");
        try {
            executeStatements(3);
            recordedQueries.verifyAtMost(2, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExactThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatement();
        recordedQueries.verify(1, Sniffer.CURRENT_THREAD);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThanThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyAtLeast(2, Sniffer.CURRENT_THREAD);
        recordedQueries.verifyAtLeast(1, Sniffer.CURRENT_THREAD);
        try {
            recordedQueries.verifyAtLeast(3, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRangeThreadLocal() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyBetween(2, 2, Sniffer.CURRENT_THREAD);
        recordedQueries.verifyBetween(1, 2, Sniffer.CURRENT_THREAD);
        recordedQueries.verifyBetween(2, 3, Sniffer.CURRENT_THREAD);
        recordedQueries.verifyBetween(1, 3, Sniffer.CURRENT_THREAD);
        try {
            recordedQueries.verifyBetween(3, 4, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyBetween(0, 1, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyNever(Sniffer.OTHER_THREADS);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verifyNever(Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce(Sniffer.OTHER_THREADS);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMostOnce(Sniffer.OTHER_THREADS);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verifyAtMostOnce(Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMost(2, Sniffer.OTHER_THREADS);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMost(2, Sniffer.OTHER_THREADS);
        recordedQueries = new SpyWithValue<>("val");
        recordedQueries.verifyAtMost(2, Sniffer.OTHER_THREADS);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verifyAtMost(2, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExactOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatementInOtherThread();
        recordedQueries.verify(1, Sniffer.OTHER_THREADS);
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new SpyWithValue<>("val");
        try {
            recordedQueries.verify(1, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThanOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatementsInOtherThread(2);
        recordedQueries.verifyAtLeast(2, Sniffer.OTHER_THREADS);
        recordedQueries.verifyAtLeast(1, Sniffer.OTHER_THREADS);
        try {
            recordedQueries.verifyAtLeast(3, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRangeOtherThreads() throws Exception {
        SpyWithValue<String> recordedQueries = new SpyWithValue<>("val");
        executeStatementsInOtherThread(2);
        recordedQueries.verifyBetween(2, 2, Sniffer.OTHER_THREADS);
        recordedQueries.verifyBetween(1, 2, Sniffer.OTHER_THREADS);
        recordedQueries.verifyBetween(2, 3, Sniffer.OTHER_THREADS);
        recordedQueries.verifyBetween(1, 3, Sniffer.OTHER_THREADS);
        try {
            recordedQueries.verifyBetween(3, 4, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyBetween(0, 1, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }


}