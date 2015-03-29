package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class OtherThreadsSnifferTest extends BaseTest {

    @Test
    public void testVerifyExact() throws Exception {
        OtherThreadsSniffer.reset();
        executeStatementInOtherThread();
        OtherThreadsSniffer.verifyExact(1);
    }

    @Test
    public void testVerifyNotLessThan() throws Exception {
        // test positive case 1
        OtherThreadsSniffer.reset();
        executeStatementInOtherThread();
        OtherThreadsSniffer.verifyNotLessThan(1);

        // test positive case 2
        executeStatementInOtherThread();
        OtherThreadsSniffer.verifyNotLessThan(0);

        // test positive case 3
        executeStatementsInOtherThread(2);
        OtherThreadsSniffer.verifyNotLessThan(1);

        // test positive case 4
        OtherThreadsSniffer.verifyNotLessThan(0);

        // test negative
        try {
            OtherThreadsSniffer.verifyNotLessThan(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

}