package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static com.github.bedrin.jdbc.sniffer.Query.INSERT;
import static com.github.bedrin.jdbc.sniffer.Query.SELECT;
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

}