package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class RecordedQueriesWithValueTest {

    @Test
    public void testVerifyNotMore() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNoMoreQueries();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNoMoreQueries();
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
        Sniffer.executeStatement();
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
        Sniffer.executeStatement();
        Sniffer.executeStatement();
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
        Sniffer.executeStatement();
        Sniffer.executeStatement();
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
        recordedQueries.verifyNoMoreThreadLocalQueries();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNoMoreThreadLocalQueries();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOneThreadLocal();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOneThreadLocal();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNotMoreThanOneThreadLocal();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanThreadLocal(2);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanThreadLocal(2);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanThreadLocal(2);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNotMoreThanThreadLocal(2);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExactThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        Sniffer.executeStatement();
        recordedQueries.verifyExactThreadLocal(1);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExactThreadLocal(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExactThreadLocal(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThanThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        Sniffer.executeStatement();
        Sniffer.executeStatement();
        recordedQueries.verifyNotLessThanThreadLocal(2);
        recordedQueries.verifyNotLessThanThreadLocal(1);
        try {
            recordedQueries.verifyNotLessThanThreadLocal(3);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRangeThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        Sniffer.executeStatement();
        Sniffer.executeStatement();
        recordedQueries.verifyRangeThreadLocal(2, 2);
        recordedQueries.verifyRangeThreadLocal(1, 2);
        recordedQueries.verifyRangeThreadLocal(2, 3);
        recordedQueries.verifyRangeThreadLocal(1, 3);
        try {
            recordedQueries.verifyRangeThreadLocal(3, 4);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyRangeThreadLocal(0, 1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNoMoreOtherThreadsQueries();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNoMoreOtherThreadsQueries();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOneOtherThreads();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOneOtherThreads();
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNotMoreThanOneOtherThreads();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOtherThreads(2);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOtherThreads(2);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        recordedQueries.verifyNotMoreThanOtherThreads(2);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyNotMoreThanOtherThreads(2);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyExactOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        Thread t = new Thread(Sniffer::executeStatement);
        t.start();
        t.join();
        recordedQueries.verifyExactOtherThreads(1);
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExactOtherThreads(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueriesWithValue<>("val");
        try {
            recordedQueries.verifyExactOtherThreads(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotLessThanOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        Thread t1 = new Thread(Sniffer::executeStatement);
        Thread t2 = new Thread(Sniffer::executeStatement);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        recordedQueries.verifyNotLessThanOtherThreads(2);
        recordedQueries.verifyNotLessThanOtherThreads(1);
        try {
            recordedQueries.verifyNotLessThanOtherThreads(3);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyRangeOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val");
        Thread t1 = new Thread(Sniffer::executeStatement);
        Thread t2 = new Thread(Sniffer::executeStatement);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        recordedQueries.verifyRangeOtherThreads(2, 2);
        recordedQueries.verifyRangeOtherThreads(1, 2);
        recordedQueries.verifyRangeOtherThreads(2, 3);
        recordedQueries.verifyRangeOtherThreads(1, 3);
        try {
            recordedQueries.verifyRangeOtherThreads(3, 4);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyRangeOtherThreads(0, 1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }


}