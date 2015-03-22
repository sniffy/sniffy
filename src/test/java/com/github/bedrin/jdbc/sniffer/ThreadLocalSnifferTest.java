package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ThreadLocalSnifferTest {

    @Test
    public void testResetImpl() throws Exception {
        ThreadLocalSniffer.reset();
        assertEquals(0, ThreadLocalSniffer.executedStatements());
        Sniffer.executeStatement();
        ThreadLocalSniffer.reset();
        assertEquals(0, ThreadLocalSniffer.executedStatements());
    }

    @Test
    public void testExecuteStatement() throws Exception {
        ThreadLocalSniffer.reset();
        assertEquals(0, ThreadLocalSniffer.executedStatements());
        Sniffer.executeStatement();
        assertEquals(1, ThreadLocalSniffer.executedStatements());
        Sniffer.executeStatement();
        assertEquals(2, ThreadLocalSniffer.executedStatements());
    }

    @Test
    public void testVerifyExact() throws Exception {
        // test positive case 1
        ThreadLocalSniffer.reset();
        Sniffer.executeStatement();
        ThreadLocalSniffer.verifyExact(1);

        // test positive case 2
        Sniffer.executeStatement();
        Thread thread = new Thread(Sniffer::executeStatement);
        thread.start();
        thread.join();
        ThreadLocalSniffer.verifyExact(1);

        // test negative case 1
        try {
            ThreadLocalSniffer.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }

        // test negative case 2
        Sniffer.executeStatement();
        Sniffer.executeStatement();
        try {
            ThreadLocalSniffer.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

}