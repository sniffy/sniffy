package com.github.bedrin.jdbc.sniffer;

import com.github.bedrin.jdbc.sniffer.sql.Query;
import org.junit.Ignore;
import org.junit.Test;

public class SnifferExpectApiTest extends BaseTest {

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testNotMoreThan() throws Exception {
        try (Spy eq = Sniffer.expectAtMost(1)) {
            executeStatements(2);
        }
    }

    @Test
    @Ignore
    public void testNotMoreThanOneInsert() throws Exception {
        try (Spy eq = Sniffer.expectAtMost(1, Query.Type.INSERT)) {
            executeStatement(Query.Type.INSERT);
            executeStatement(Query.Type.UPDATE);
        }
    }

    @Test
    public void testNotMoreThanAllThreads() throws Exception {
        try (Spy eq = Sniffer.expectAtMost(1, Threads.CURRENT)) {
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testNotMoreThanOtherThreads() throws Exception {
        try (Spy eq = Sniffer.expectAtMost(1, Threads.OTHERS)) {
            executeStatementInOtherThread();
            executeStatements(2);
        }
    }

}