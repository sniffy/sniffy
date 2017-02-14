package io.sniffy.test.junit;

import io.sniffy.*;
import io.sniffy.junit.QueryCounter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ThreadLocalQueryCounterTest extends BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public QueryCounter queryCounter = new QueryCounter();

    @Test
    @Expectation(value = 1, threads = Threads.CURRENT)
    public void testAllowedOneQuery() {
        BaseTest.executeStatement();
    }

    @Test
    public void testNoExpectations() {
        BaseTest.executeStatement();
    }

    @Test
    @NoQueriesAllowed()
    public void testNotAllowedQueries() {
        BaseTest.executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(value = 1, threads = Threads.CURRENT)
    public void testAllowedOneQueryExecutedTwo() {
        BaseTest.executeStatements(2);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    public void testWithoutExpectations() {
        Assert.assertTrue(true);
    }

    @Test
    @Expectation(atLeast = 1, threads = Threads.CURRENT)
    public void testAllowedMinOneQueryExecutedTwo() {
        BaseTest.executeStatements(2);
    }

    @Test
    @Expectation(atLeast = 2, threads = Threads.CURRENT)
    public void testAllowedMinTwoQueriesExecutedOne() {
        BaseTest.executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(value = 2, threads = Threads.CURRENT)
    public void testAllowedExactTwoQueriesExecutedTwo() {
        BaseTest.executeStatements(2);
    }

    @Test
    @Expectation(value = 2, threads = Threads.CURRENT)
    public void testAllowedExactTwoQueriesExecutedThree() {
        BaseTest.executeStatements(3);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(value = 2, threads = Threads.CURRENT)
    public void testAllowedTwoQueries() {
        BaseTest.executeStatements(2);
    }

    @Test
    @Expectations({
            @Expectation(value = 1, query = Query.SELECT, threads = Threads.CURRENT),
            @Expectation(value = 1, query = Query.INSERT, threads = Threads.CURRENT),
            @Expectation(value = 1, query = Query.UPDATE, threads = Threads.CURRENT),
            @Expectation(value = 1, query = Query.DELETE, threads = Threads.CURRENT),
            @Expectation(value = 1, query = Query.MERGE, threads = Threads.OTHERS)
    })
    public void testDifferentQueries() {
        BaseTest.executeStatement(Query.SELECT);
        BaseTest.executeStatement(Query.INSERT);
        BaseTest.executeStatement(Query.UPDATE);
        BaseTest.executeStatement(Query.DELETE);
        BaseTest.executeStatementInOtherThread(Query.MERGE);
    }

}