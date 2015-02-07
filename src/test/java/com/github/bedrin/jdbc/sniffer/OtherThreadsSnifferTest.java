package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class OtherThreadsSnifferTest {

    @Test
    public void testVerifyExact() throws Exception {
        OtherThreadsSniffer.reset();
        Thread thread = new Thread(Sniffer::executeStatement);
        thread.start();
        thread.join();
        OtherThreadsSniffer.verifyExact(1);
    }

    @Test
    public void testVerifyNotLessThan() throws Exception {
        // test positive case 1
        OtherThreadsSniffer.reset();
        Thread thread = new Thread(Sniffer::executeStatement);
        thread.start();
        thread.join();
        OtherThreadsSniffer.verifyNotLessThan(1);

        // test positive case 2
        thread = new Thread(Sniffer::executeStatement);
        thread.start();
        thread.join();
        OtherThreadsSniffer.verifyNotLessThan(0);

        // test positive case 3
        thread = new Thread(Sniffer::executeStatement);
        thread.start();
        thread.join();
        thread = new Thread(Sniffer::executeStatement);
        thread.start();
        thread.join();
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