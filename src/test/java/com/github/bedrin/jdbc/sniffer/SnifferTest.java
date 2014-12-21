package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class SnifferTest {

    @Test
    public void testResetImpl() throws Exception {
        Sniffer.reset();
        assertEquals(0, Sniffer.executedStatements());
        Sniffer.executeStatement();
        Sniffer.reset();
        assertEquals(0, Sniffer.executedStatements());
    }

    @Test
    public void testExecuteStatement() throws Exception {
        Sniffer.reset();
        assertEquals(0, Sniffer.executedStatements());
        Sniffer.executeStatement();
        assertEquals(1, Sniffer.executedStatements());
    }

    @Test
    public void testRecordQueriesPositive() throws Exception {
        Sniffer.recordQueries(new Runnable() {

            @Override
            public void run() {
                Sniffer.executeStatement();
            }

        }).verifyNotMoreThanOne();
    }

    @Test
    public void testRecordQueriesNegative() throws Exception {
        try {
            Sniffer.recordQueries(new Runnable() {

                @Override
                public void run() {
                    Sniffer.executeStatement();
                }

            }).verifyNotMore();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }

}