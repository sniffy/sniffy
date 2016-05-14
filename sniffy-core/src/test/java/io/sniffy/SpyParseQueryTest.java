package io.sniffy;

import org.junit.Test;

public class SpyParseQueryTest extends BaseTest {

    @Test
    public void testNeverInsertPositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatement(Query.SELECT);
        spy.verifyNever(Query.INSERT);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testNeverInsertNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatement(Query.INSERT);
        spy.verifyNever(Query.INSERT);
    }

    @Test
    public void testNeverInsertOtherThreadPositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementInOtherThread(Query.SELECT);
        executeStatement(Query.INSERT);
        spy.verifyNever(Threads.OTHERS, Query.INSERT);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testNeverInsertOtherThreadNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementInOtherThread(Query.INSERT);
        spy.verifyNever(Query.INSERT, Threads.OTHERS);
    }

    @Test
    public void testAtMostOnceUpdatePositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatement(Query.UPDATE);
        spy.verifyAtMostOnce(Query.UPDATE);
        spy = Sniffy.spy();
        executeStatement(Query.DELETE);
        executeStatement(Query.UPDATE);
        spy.verifyAtMostOnce(Query.UPDATE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostOnceUpdateNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatements(2, Query.UPDATE);
        spy.verifyAtMostOnce(Query.UPDATE);
    }

    @Test
    public void testAtMostOnceUpdateOtherThreadPositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementInOtherThread(Query.SELECT);
        executeStatementInOtherThread(Query.UPDATE);
        executeStatements(5, Query.UPDATE);
        spy.verifyAtMostOnce(Threads.OTHERS, Query.UPDATE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostOnceUpdateOtherThreadNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementsInOtherThread(2, Query.UPDATE);
        spy.verifyAtMostOnce(Query.UPDATE, Threads.OTHERS);
    }

    @Test
    public void testAtMostMergePositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatement(Query.MERGE);
        spy.verifyAtMost(2, Query.MERGE);
        spy = Sniffy.spy();
        executeStatement(Query.DELETE);
        executeStatements(2, Query.MERGE);
        spy.verifyAtMost(2, Query.MERGE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostMergeNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatements(3, Query.MERGE);
        spy.verifyAtMost(2, Query.MERGE);
    }

    @Test
    public void testAtMostMergeOtherThreadPositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementInOtherThread(Query.SELECT);
        executeStatementInOtherThread(Query.MERGE);
        executeStatements(5, Query.MERGE);
        spy.verifyAtMost(2, Threads.OTHERS, Query.MERGE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostMergeOtherThreadNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementsInOtherThread(3, Query.MERGE);
        spy.verifyAtMost(2, Query.MERGE, Threads.OTHERS);
    }

    @Test
    public void testAtLeastDeletePositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatements(3, Query.DELETE);
        spy.verifyAtLeast(2, Query.DELETE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtLeastDeleteNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatement(Query.DELETE);
        executeStatement(Query.OTHER);
        spy.verifyAtLeast(2, Query.DELETE);
    }

    @Test
    public void testAtLeastDeleteOtherThreadPositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementsInOtherThread(2, Query.DELETE);
        spy.verifyAtLeast(2, Threads.OTHERS, Query.DELETE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtLeastDeleteOtherThreadNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementInOtherThread(Query.DELETE);
        executeStatement(Query.DELETE);
        spy.verifyAtLeast(2, Query.DELETE, Threads.OTHERS);
    }

    @Test
    public void testBetweenOtherPositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatements(3, Query.OTHER);
        spy.verifyBetween(2, 4, Query.OTHER);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testBetweenOtherNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatements(5, Query.OTHER);
        spy.verifyBetween(2, 4, Query.OTHER);
    }

    @Test
    public void testBetweenOtherOtherThreadPositive() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementsInOtherThread(2, Query.OTHER);
        spy.verifyBetween(2, 4, Threads.OTHERS, Query.OTHER);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testBetweenOtherOtherThreadNegative() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementInOtherThread(Query.OTHER);
        executeStatements(2, Query.OTHER);
        spy.verifyBetween(2, 4, Query.OTHER, Threads.OTHERS);
    }

}