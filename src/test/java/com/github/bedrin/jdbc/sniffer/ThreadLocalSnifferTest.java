package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThreadLocalSnifferTest {

    @Test
    public void testResetImpl() throws Exception {
        ThreadLocalSniffer.reset();
        assertEquals(0, ThreadLocalSniffer.executedStatements());
        ThreadLocalSniffer.executeStatement();
        ThreadLocalSniffer.reset();
        assertEquals(0, ThreadLocalSniffer.executedStatements());
    }

    @Test
    public void testExecuteStatement() throws Exception {
        ThreadLocalSniffer.reset();
        assertEquals(0, ThreadLocalSniffer.executedStatements());
        ThreadLocalSniffer.executeStatement();
        assertEquals(1, ThreadLocalSniffer.executedStatements());
        Sniffer.executeStatement();
        assertEquals(2, ThreadLocalSniffer.executedStatements());
    }

}