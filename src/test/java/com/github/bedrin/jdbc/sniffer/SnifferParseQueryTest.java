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
    public void testAtMostOnceInsertPositive() throws Exception {
        try (Spy ignored = Sniffer.expectAtMostOnce(INSERT)) {
            executeStatement(UPDATE);
        }
        try (Spy ignored = Sniffer.expectAtMostOnce(INSERT)) {
            executeStatement(DELETE);
            executeStatement(INSERT);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostOnceInsertNegative() throws Exception {
        try (Spy ignored = Sniffer.expectAtMostOnce(INSERT)) {
            executeStatements(2, INSERT);
        }
    }

    @Test
    public void testAtMostOnceInsertOtherThreadPositive() throws Exception {
        try (Spy ignored = Sniffer.expectAtMostOnce(Threads.OTHERS, INSERT)) {
            executeStatementInOtherThread(SELECT);
            executeStatementInOtherThread(INSERT);
            executeStatements(5, INSERT);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostOnceInsertOtherThreadNegative() throws Exception {
        try (Spy ignored = Sniffer.expectAtMostOnce(INSERT, Threads.OTHERS)) {
            executeStatementsInOtherThread(2,INSERT);
        }
    }

}