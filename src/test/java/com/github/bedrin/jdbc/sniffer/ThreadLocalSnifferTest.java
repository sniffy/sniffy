package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ThreadLocalSnifferTest extends BaseTest {

    @Test
    public void testVerifyExact() throws Exception {
        // test positive case 1
        Spy spy = Sniffer.spy();
        executeStatement();
        spy.verify(1, Sniffer.CURRENT_THREAD);

        // test positive case 2
        spy = Sniffer.spy();
        executeStatement();
        executeStatementInOtherThread();
        spy.verify(1, Sniffer.CURRENT_THREAD);

        // test negative case 1
        spy = Sniffer.spy();
        try {
            spy.verify(1, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }

        // test negative case 2
        spy = Sniffer.spy();
        executeStatement();
        executeStatement();
        try {
            spy.verify(2, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

}