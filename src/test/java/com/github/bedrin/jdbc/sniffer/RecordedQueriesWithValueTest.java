package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class RecordedQueriesWithValueTest extends BaseTest {

    @Test
    public void testVerifyNotMore() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNoMore();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNoMore();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOne() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOne();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOne();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNotMoreThanOne();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThan() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThan(2);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThan(2);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThan(2);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNotMoreThan(2);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExact() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatement();
        recordedQueries.verifyExact(1);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThan() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyNotLessThan(2);
        recordedQueries.verifyNotLessThan(1);
        try {
            recordedQueries.verifyNotLessThan(3);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRange() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyRange(2, 2);
        recordedQueries.verifyRange(1, 2);
        recordedQueries.verifyRange(2, 3);
        recordedQueries.verifyRange(1, 3);
        try {
            recordedQueries.verifyRange(3,4);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyRange(0,1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNoMore(Sniffer.CURRENT_THREAD);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNoMore(Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOne(Sniffer.CURRENT_THREAD);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOne(Sniffer.CURRENT_THREAD);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNotMoreThanOne(Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatementsInOtherThread(3);
        recordedQueries.verifyNotMoreThan(2, Sniffer.CURRENT_THREAD);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            executeStatements(3);
            recordedQueries.verifyNotMoreThan(2, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExactThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatement();
        recordedQueries.verifyExact(1, Sniffer.CURRENT_THREAD);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExact(1, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExact(1, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThanThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyNotLessThan(2, Sniffer.CURRENT_THREAD);
        recordedQueries.verifyNotLessThan(1, Sniffer.CURRENT_THREAD);
        try {
            recordedQueries.verifyNotLessThan(3, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRangeThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatements(2);
        recordedQueries.verifyRange(2, 2, Sniffer.CURRENT_THREAD);
        recordedQueries.verifyRange(1, 2, Sniffer.CURRENT_THREAD);
        recordedQueries.verifyRange(2, 3, Sniffer.CURRENT_THREAD);
        recordedQueries.verifyRange(1, 3, Sniffer.CURRENT_THREAD);
        try {
            recordedQueries.verifyRange(3, 4, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyRange(0, 1, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNoMore(Sniffer.OTHER_THREADS);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNoMore(Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOne(Sniffer.OTHER_THREADS);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOne(Sniffer.OTHER_THREADS);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNotMoreThanOne(Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThan(2, Sniffer.OTHER_THREADS);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThan(2, Sniffer.OTHER_THREADS);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThan(2, Sniffer.OTHER_THREADS);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNotMoreThan(2, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExactOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatementInOtherThread();
        recordedQueries.verifyExact(1, Sniffer.OTHER_THREADS);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExact(1, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExact(1, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThanOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatementsInOtherThread(2);
        recordedQueries.verifyNotLessThan(2, Sniffer.OTHER_THREADS);
        recordedQueries.verifyNotLessThan(1, Sniffer.OTHER_THREADS);
        try {
            recordedQueries.verifyNotLessThan(3, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRangeOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        executeStatementsInOtherThread(2);
        recordedQueries.verifyRange(2, 2, Sniffer.OTHER_THREADS);
        recordedQueries.verifyRange(1, 2, Sniffer.OTHER_THREADS);
        recordedQueries.verifyRange(2, 3, Sniffer.OTHER_THREADS);
        recordedQueries.verifyRange(1, 3, Sniffer.OTHER_THREADS);
        try {
            recordedQueries.verifyRange(3, 4, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyRange(0, 1, Sniffer.OTHER_THREADS);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }


}