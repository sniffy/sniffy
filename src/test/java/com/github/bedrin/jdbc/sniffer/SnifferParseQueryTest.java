package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static com.github.bedrin.jdbc.sniffer.Query.*;
import static org.junit.Assert.*;

public class SnifferParseQueryTest extends BaseTest {

    @Test
    public void testNeverInsertPositive() throws Exception {
        try (Spy ignored = Sniffer.expectNever(INSERT)) {
            executeStatement(SELECT);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testNeverInsertNegative() throws Exception {
        try (Spy ignored = Sniffer.expectNever(INSERT)) {
            executeStatement(INSERT);
        }
    }

    @Test
    public void testNeverInsertOtherThreadPositive() throws Exception {
        try (Spy ignored = Sniffer.expectNever(Threads.OTHERS, INSERT)) {
            executeStatementInOtherThread(SELECT);
            executeStatement(INSERT);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testNeverInsertOtherThreadNegative() throws Exception {
        try (Spy ignored = Sniffer.expectNever(INSERT, Threads.OTHERS)) {
            executeStatementInOtherThread(INSERT);
        }
    }

    @Test
    public void testAtMostOnceUpdatePositive() throws Exception {
        try (Spy ignored = Sniffer.expectAtMostOnce(UPDATE)) {
            executeStatement(UPDATE);
        }
        try (Spy ignored = Sniffer.expectAtMostOnce(UPDATE)) {
            executeStatement(DELETE);
            executeStatement(UPDATE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostOnceUpdateNegative() throws Exception {
        try (Spy ignored = Sniffer.expectAtMostOnce(UPDATE)) {
            executeStatements(2, UPDATE);
        }
    }

    @Test
    public void testAtMostOnceUpdateOtherThreadPositive() throws Exception {
        try (Spy ignored = Sniffer.expectAtMostOnce(Threads.OTHERS, UPDATE)) {
            executeStatementInOtherThread(SELECT);
            executeStatementInOtherThread(UPDATE);
            executeStatements(5, UPDATE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostOnceUpdateOtherThreadNegative() throws Exception {
        try (Spy ignored = Sniffer.expectAtMostOnce(UPDATE, Threads.OTHERS)) {
            executeStatementsInOtherThread(2,UPDATE);
        }
    }

}