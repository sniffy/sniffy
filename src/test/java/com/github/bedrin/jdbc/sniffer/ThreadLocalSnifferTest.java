package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ThreadLocalSnifferTest extends BaseTest {

    @Test
    public void testExecuteStatement() throws Exception {
        int c = ThreadLocalSniffer.executedStatements();
        executeStatement();
        assertEquals(1, ThreadLocalSniffer.executedStatements() - c);
        executeStatement();
        assertEquals(2, ThreadLocalSniffer.executedStatements() - c);
    }

    @Test
    public void testVerifyExact() throws Exception {
        // test positive case 1
        ExpectedQueries expectedQueries = Sniffer.expectedQueries();
        executeStatement();
        expectedQueries.verifyExact(1, Sniffer.CURRENT_THREAD);

        // test positive case 2
        expectedQueries = Sniffer.expectedQueries();
        executeStatement();
        executeStatementInOtherThread();
        expectedQueries.verifyExact(1, Sniffer.CURRENT_THREAD);

        // test negative case 1
        expectedQueries = Sniffer.expectedQueries();
        try {
            expectedQueries.verifyExact(1, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }

        // test negative case 2
        expectedQueries = Sniffer.expectedQueries();
        executeStatement();
        executeStatement();
        try {
            expectedQueries.verifyExact(2, Sniffer.CURRENT_THREAD);
            fail();
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

}