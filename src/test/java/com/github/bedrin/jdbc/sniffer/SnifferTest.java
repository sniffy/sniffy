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

}