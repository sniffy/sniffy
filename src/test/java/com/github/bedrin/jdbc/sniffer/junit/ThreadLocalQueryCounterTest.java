package com.github.bedrin.jdbc.sniffer.junit;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import com.github.bedrin.jdbc.sniffer.Threads;
import com.github.bedrin.jdbc.sniffer.WrongNumberOfQueriesError;
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

}