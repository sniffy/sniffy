package io.sniffy;

import org.junit.Test;

import static io.sniffy.Query.*;

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
            executeStatementsInOtherThread(2, UPDATE);
        }
    }

    @Test
    public void testAtMostMergePositive() throws Exception {
        try (Spy ignored = Sniffer.expectAtMost(2, MERGE)) {
            executeStatement(MERGE);
        }
        try (Spy ignored = Sniffer.expectAtMost(2, MERGE)) {
            executeStatement(DELETE);
            executeStatements(2, MERGE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostMergeNegative() throws Exception {
        try (Spy ignored = Sniffer.expectAtMost(2, MERGE)) {
            executeStatements(3, MERGE);
        }
    }

    @Test
    public void testAtMostMergeOtherThreadPositive() throws Exception {
        try (Spy ignored = Sniffer.expectAtMost(2, Threads.OTHERS, MERGE)) {
            executeStatementInOtherThread(SELECT);
            executeStatementInOtherThread(MERGE);
            executeStatements(5, MERGE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostMergeOtherThreadNegative() throws Exception {
        try (Spy ignored = Sniffer.expectAtMost(2, MERGE, Threads.OTHERS)) {
            executeStatementsInOtherThread(3, MERGE);
        }
    }

    @Test
    public void testAtLeastDeletePositive() throws Exception {
        try (Spy ignored = Sniffer.expectAtLeast(2, DELETE)) {
            executeStatements(3, DELETE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtLeastDeleteNegative() throws Exception {
        try (Spy ignored = Sniffer.expectAtLeast(2, DELETE)) {
            executeStatement(DELETE);
            executeStatement(OTHER);
        }
    }

    @Test
    public void testAtLeastDeleteOtherThreadPositive() throws Exception {
        try (Spy ignored = Sniffer.expectAtLeast(2, Threads.OTHERS, DELETE)) {
            executeStatementsInOtherThread(2, DELETE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtLeastDeleteOtherThreadNegative() throws Exception {
        try (Spy ignored = Sniffer.expectAtLeast(2, DELETE, Threads.OTHERS)) {
            executeStatementInOtherThread(DELETE);
            executeStatement(DELETE);
        }
    }

    @Test
    public void testBetweenOtherPositive() throws Exception {
        try (Spy ignored = Sniffer.expectBetween(2, 4, OTHER)) {
            executeStatements(3, OTHER);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testBetweenOtherNegative() throws Exception {
        try (Spy ignored = Sniffer.expectBetween(2, 4, OTHER)) {
            executeStatements(5, OTHER);
        }
    }

    @Test
    public void testBetweenOtherOtherThreadPositive() throws Exception {
        try (Spy ignored = Sniffer.expectBetween(1, 3, Threads.OTHERS, OTHER)) {
            executeStatementsInOtherThread(2, OTHER);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testBetweenOtherOtherThreadNegative() throws Exception {
        try (Spy ignored = Sniffer.expectBetween(2, 4, OTHER, Threads.OTHERS)) {
            executeStatementInOtherThread(OTHER);
            executeStatements(2, OTHER);
        }
    }

}