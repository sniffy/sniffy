package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ThreadLocalSnifferTest extends BaseTest {

    @Test
    public void testResetImpl() throws Exception {
        ThreadLocalSniffer.reset();
        assertEquals(0, ThreadLocalSniffer.executedStatements());
        executeStatement();
        ThreadLocalSniffer.reset();
        assertEquals(0, ThreadLocalSniffer.executedStatements());
    }

    @Test
    public void testExecuteStatement() throws Exception {
        ThreadLocalSniffer.reset();
        assertEquals(0, ThreadLocalSniffer.executedStatements());
        executeStatement();
        assertEquals(1, ThreadLocalSniffer.executedStatements());
        executeStatement();
        assertEquals(2, ThreadLocalSniffer.executedStatements());
    }

    @Test
    public void testVerifyExact() throws Exception {
        // test positive case 1
        ThreadLocalSniffer.reset();
        executeStatement();
        ThreadLocalSniffer.verifyExact(1);

        // test positive case 2
        executeStatement();
        Thread thread = new Thread(BaseTest::executeStatement);
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
        executeStatement();
        executeStatement();
        try {
            ThreadLocalSniffer.verifyExact(1);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

}