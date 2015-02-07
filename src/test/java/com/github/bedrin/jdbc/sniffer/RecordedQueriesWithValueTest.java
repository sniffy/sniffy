package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class RecordedQueriesWithValueTest {

    @Test
    public void testVerifyNotMore() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        recordedQueries.verifyNotMore();
        recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        try {
            recordedQueries.verifyNotMore();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOne() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        recordedQueries.verifyNotMoreThanOne();
        recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        recordedQueries.verifyNotMoreThanOne();
        recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        recordedQueries.verifyNotMoreThan(2);
        recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        recordedQueries.verifyNotMoreThan(2);
        recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
        recordedQueries.verifyNotMoreThan(2);
        recordedQueries = new RecordedQueriesWithValue<>("val",3,3,3);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        recordedQueries.verifyExact(1);
        recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        try {
            recordedQueries.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        recordedQueries.verifyNotMoreThreadLocal();
        recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        try {
            recordedQueries.verifyNotMoreThreadLocal();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneThreadLocal() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        recordedQueries.verifyNotMoreThanOneThreadLocal();
        recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        recordedQueries.verifyNotMoreThanOneThreadLocal();
        recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        recordedQueries.verifyNotMoreThanThreadLocal(2);
        recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        recordedQueries.verifyNotMoreThanThreadLocal(2);
        recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
        recordedQueries.verifyNotMoreThanThreadLocal(2);
        recordedQueries = new RecordedQueriesWithValue<>("val",3,3,3);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        recordedQueries.verifyExactThreadLocal(1);
        recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        try {
            recordedQueries.verifyExactThreadLocal(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        recordedQueries.verifyNotMoreOtherThreads();
        recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        try {
            recordedQueries.verifyNotMoreOtherThreads();
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        assertEquals("val", recordedQueries.getValue());
    }

    @Test
    public void testVerifyNotMoreThanOneOtherThreads() throws Exception {
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        recordedQueries.verifyNotMoreThanOneOtherThreads();
        recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        recordedQueries.verifyNotMoreThanOneOtherThreads();
        recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        recordedQueries.verifyNotMoreThanOtherThreads(2);
        recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        recordedQueries.verifyNotMoreThanOtherThreads(2);
        recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
        recordedQueries.verifyNotMoreThanOtherThreads(2);
        recordedQueries = new RecordedQueriesWithValue<>("val",3,3,3);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",1,1,1);
        recordedQueries.verifyExactOtherThreads(1);
        recordedQueries = new RecordedQueriesWithValue<>("val",0,0,0);
        try {
            recordedQueries.verifyExactOtherThreads(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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
        RecordedQueriesWithValue<String> recordedQueries = new RecordedQueriesWithValue<>("val",2,2,2);
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