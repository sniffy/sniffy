package io.sniffy.junit;

import io.sniffy.*;
import io.sniffy.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class ThreadLocalQueryCounterTest extends BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public QueryCounter queryCounter = new QueryCounter();

    @Test
    @Expectation(value = 1, threads = Threads.CURRENT)
    public void testAllowedOneQuery() {
        executeStatement();
    }

    @Test
    public void testNoExpectations() {
        executeStatement();
    }

    @Test
    @NoQueriesAllowed()
    public void testNotAllowedQueries() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(value = 1, threads = Threads.CURRENT)
    public void testAllowedOneQueryExecutedTwo() {
        executeStatements(2);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    public void testWithoutExpectations() {
        assertTrue(true);
    }

    @Test
    @Expectation(atLeast = 1, threads = Threads.CURRENT)
    public void testAllowedMinOneQueryExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @Expectation(atLeast = 2, threads = Threads.CURRENT)
    public void testAllowedMinTwoQueriesExecutedOne() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(value = 2, threads = Threads.CURRENT)
    public void testAllowedExactTwoQueriesExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @Expectation(value = 2, threads = Threads.CURRENT)
    public void testAllowedExactTwoQueriesExecutedThree() {
        executeStatements(3);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(value = 2, threads = Threads.CURRENT)
    public void testAllowedTwoQueries() {
        executeStatements(2);
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
        executeStatement(Query.SELECT);
        executeStatement(Query.INSERT);
        executeStatement(Query.UPDATE);
        executeStatement(Query.DELETE);
        executeStatementInOtherThread(Query.MERGE);
    }

}