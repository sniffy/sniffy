package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class RecordedQueriesTest {

    @Test
    public void testVerifyNotMore() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(0,0,0);
        recordedQueries.verifyNotMore();
        recordedQueries = new RecordedQueries(1,1,1);
        try {
            recordedQueries.verifyNotMore();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyNotMoreThanOne() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(0,0,0);
        recordedQueries.verifyNotMoreThanOne();
        recordedQueries = new RecordedQueries(1,1,1);
        recordedQueries.verifyNotMoreThanOne();
        recordedQueries = new RecordedQueries(2,2,2);
        try {
            recordedQueries.verifyNotMoreThanOne();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyNotMoreThan() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(0,0,0);
        recordedQueries.verifyNotMoreThan(2);
        recordedQueries = new RecordedQueries(1,1,1);
        recordedQueries.verifyNotMoreThan(2);
        recordedQueries = new RecordedQueries(2,2,2);
        recordedQueries.verifyNotMoreThan(2);
        recordedQueries = new RecordedQueries(3,3,3);
        try {
            recordedQueries.verifyNotMoreThan(2);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyExact() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(1,1,1);
        recordedQueries.verifyExact(1);
        recordedQueries = new RecordedQueries(0,0,0);
        try {
            recordedQueries.verifyExact(1);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueries(2,2,2);
        try {
            recordedQueries.verifyExact(1);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyNotLessThan() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(2,2,2);
        recordedQueries.verifyNotLessThan(2);
        recordedQueries.verifyNotLessThan(1);
        try {
            recordedQueries.verifyNotLessThan(3);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyRange() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(2,2,2);
        recordedQueries.verifyRange(2, 2);
        recordedQueries.verifyRange(1, 2);
        recordedQueries.verifyRange(2, 3);
        recordedQueries.verifyRange(1, 3);
        try {
            recordedQueries.verifyRange(3,4);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyRange(0,1);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyNotMoreThreadLocal() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(0,0,0);
        recordedQueries.verifyNotMoreThreadLocal();
        recordedQueries = new RecordedQueries(1,1,1);
        try {
            recordedQueries.verifyNotMoreThreadLocal();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyNotMoreThanOneThreadLocal() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(0,0,0);
        recordedQueries.verifyNotMoreThanOneThreadLocal();
        recordedQueries = new RecordedQueries(1,1,1);
        recordedQueries.verifyNotMoreThanOneThreadLocal();
        recordedQueries = new RecordedQueries(2,2,2);
        try {
            recordedQueries.verifyNotMoreThanOneThreadLocal();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyNotMoreThanThreadLocal() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(0,0,0);
        recordedQueries.verifyNotMoreThanThreadLocal(2);
        recordedQueries = new RecordedQueries(1,1,1);
        recordedQueries.verifyNotMoreThanThreadLocal(2);
        recordedQueries = new RecordedQueries(2,2,2);
        recordedQueries.verifyNotMoreThanThreadLocal(2);
        recordedQueries = new RecordedQueries(3,3,3);
        try {
            recordedQueries.verifyNotMoreThanThreadLocal(2);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyExactThreadLocal() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(1,1,1);
        recordedQueries.verifyExactThreadLocal(1);
        recordedQueries = new RecordedQueries(0,0,0);
        try {
            recordedQueries.verifyExactThreadLocal(1);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
        recordedQueries = new RecordedQueries(2,2,2);
        try {
            recordedQueries.verifyExactThreadLocal(1);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyNotLessThanThreadLocal() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(2,2,2);
        recordedQueries.verifyNotLessThanThreadLocal(2);
        recordedQueries.verifyNotLessThanThreadLocal(1);
        try {
            recordedQueries.verifyNotLessThanThreadLocal(3);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyRangeThreadLocal() throws Exception {
        RecordedQueries recordedQueries = new RecordedQueries(2,2,2);
        recordedQueries.verifyRangeThreadLocal(2, 2);
        recordedQueries.verifyRangeThreadLocal(1, 2);
        recordedQueries.verifyRangeThreadLocal(2, 3);
        recordedQueries.verifyRangeThreadLocal(1, 3);
        try {
            recordedQueries.verifyRangeThreadLocal(3, 4);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
        try {
            recordedQueries.verifyRangeThreadLocal(0,1);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

}