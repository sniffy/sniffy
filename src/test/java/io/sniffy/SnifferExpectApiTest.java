package io.sniffy;

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
        try (Spy $= Sniffer.expectAtMost(1, Query.INSERT)) {
            executeStatement(Query.INSERT);
            executeStatement(Query.UPDATE);
        }
    }

    @Test
    public void testExactlyOneThreadLocalInsert() throws Exception {
        try (Spy $= Sniffer.expect(1, Query.INSERT)) {
            executeStatement(Query.INSERT);
        }
    }

    @Test
    public void testExactlyOneOtherThreadInsert() throws Exception {
        try (Spy $= Sniffer.expect(1, Threads.OTHERS, Query.INSERT)) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
            executeStatements(2, Query.UPDATE);
        }
    }

    @Test
    public void testExactlyTwoInsertsAnyThread() throws Exception {
        try (Spy $= Sniffer.expect(2, Query.INSERT, Threads.ANY)) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
            executeStatements(2, Query.UPDATE);
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