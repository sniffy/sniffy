package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static com.github.bedrin.jdbc.sniffer.Query.*;

public class SpyParseQueryTest extends BaseTest {

    @Test
    public void testNeverInsertPositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatement(SELECT);
        spy.verifyNever(INSERT);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testNeverInsertNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatement(INSERT);
        spy.verifyNever(INSERT);
    }

    @Test
    public void testNeverInsertOtherThreadPositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementInOtherThread(SELECT);
        executeStatement(INSERT);
        spy.verifyNever(Threads.OTHERS, INSERT);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testNeverInsertOtherThreadNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementInOtherThread(INSERT);
        spy.verifyNever(INSERT, Threads.OTHERS);
    }

    @Test
    public void testAtMostOnceUpdatePositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatement(UPDATE);
        spy.verifyAtMostOnce(UPDATE);
        spy = Sniffer.spy();
        executeStatement(DELETE);
        executeStatement(UPDATE);
        spy.verifyAtMostOnce(UPDATE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostOnceUpdateNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatements(2, UPDATE);
        spy.verifyAtMostOnce(UPDATE);
    }

    @Test
    public void testAtMostOnceUpdateOtherThreadPositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementInOtherThread(SELECT);
        executeStatementInOtherThread(UPDATE);
        executeStatements(5, UPDATE);
        spy.verifyAtMostOnce(Threads.OTHERS, UPDATE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostOnceUpdateOtherThreadNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementsInOtherThread(2, UPDATE);
        spy.verifyAtMostOnce(UPDATE, Threads.OTHERS);
    }

    @Test
    public void testAtMostMergePositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatement(MERGE);
        spy.verifyAtMost(2, MERGE);
        spy = Sniffer.spy();
        executeStatement(DELETE);
        executeStatements(2, MERGE);
        spy.verifyAtMost(2, MERGE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostMergeNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatements(3, MERGE);
        spy.verifyAtMost(2, MERGE);
    }

    @Test
    public void testAtMostMergeOtherThreadPositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementInOtherThread(SELECT);
        executeStatementInOtherThread(MERGE);
        executeStatements(5, MERGE);
        spy.verifyAtMost(2, Threads.OTHERS, MERGE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtMostMergeOtherThreadNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementsInOtherThread(3, MERGE);
        spy.verifyAtMost(2, MERGE, Threads.OTHERS);
    }

    @Test
    public void testAtLeastDeletePositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatements(3, DELETE);
        spy.verifyAtLeast(2, DELETE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtLeastDeleteNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatement(DELETE);
        executeStatement(OTHER);
        spy.verifyAtLeast(2, DELETE);
    }

    @Test
    public void testAtLeastDeleteOtherThreadPositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementsInOtherThread(2, DELETE);
        spy.verifyAtLeast(2, Threads.OTHERS, DELETE);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testAtLeastDeleteOtherThreadNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementInOtherThread(DELETE);
        executeStatement(DELETE);
        spy.verifyAtLeast(2, DELETE, Threads.OTHERS);
    }

    @Test
    public void testBetweenOtherPositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatements(3, OTHER);
        spy.verifyBetween(2, 4, OTHER);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testBetweenOtherNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatements(5, OTHER);
        spy.verifyBetween(2, 4, OTHER);
    }

    @Test
    public void testBetweenOtherOtherThreadPositive() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementsInOtherThread(2, OTHER);
        spy.verifyBetween(2, 4, Threads.OTHERS, OTHER);
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testBetweenOtherOtherThreadNegative() throws Exception {
        Spy spy = Sniffer.spy();
        executeStatementInOtherThread(OTHER);
        executeStatements(2, OTHER);
        spy.verifyBetween(2, 4, OTHER, Threads.OTHERS);
    }

}