package com.github.bedrin.jdbc.sniffer;

import com.github.bedrin.jdbc.sniffer.sql.Query;
import org.junit.Test;

public class SnifferExpectApiTest extends BaseTest {

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testNotMoreThan() throws Exception {
        try (Spy $= Sniffer.expectAtMost(1)) {
            executeStatements(2);
        }
    }

    @Test
    public void testNotMoreThanOneInsert() throws Exception {
        try (Spy $= Sniffer.expectAtMost(1, Query.Type.INSERT)) {
            executeStatement(Query.Type.INSERT);
            executeStatement(Query.Type.UPDATE);
        }
    }

    @Test
    public void testExactlyOneThreadLocalInsert() throws Exception {
        try (Spy $= Sniffer.expect(1, Threads.CURRENT, Query.Type.INSERT)) {
            executeStatement(Query.Type.INSERT);
            executeStatementInOtherThread(Query.Type.INSERT);
            executeStatements(2, Query.Type.UPDATE);
        }
    }

    @Test
    public void testNotMoreThanAllThreads() throws Exception {
        try (Spy $= Sniffer.expectAtMost(1, Threads.CURRENT)) {
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