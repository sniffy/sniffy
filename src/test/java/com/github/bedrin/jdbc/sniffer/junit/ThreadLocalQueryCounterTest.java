package com.github.bedrin.jdbc.sniffer.junit;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ThreadLocalQueryCounterTest extends BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public QueryCounter queryCounter = new QueryCounter();

    @Test
    @AllowedQueries(value = 1, threadLocal = true)
    public void testAllowedOneQuery() {
        executeStatement();
    }

    @Test
    @NotAllowedQueries(threadLocal = true)
    public void testNotAllowedQueries() {
        executeStatement();
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(value = 1, threadLocal = true)
    public void testAllowedOneQueryExecutedTwo() {
        executeStatements(2);
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(min = 1, threadLocal = true)
    public void testAllowedMinOneQueryExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @AllowedQueries(min = 2, threadLocal = true)
    public void testAllowedMinTwoQueriesExecutedOne() {
        executeStatement();
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(exact = 2, threadLocal = true)
    public void testAllowedExactTwoQueriesExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @AllowedQueries(exact = 2, threadLocal = true)
    public void testAllowedExactTwoQueriesExecutedThree() {
        executeStatements(3);
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(value = 2, threadLocal = true)
    public void testAllowedTwoQueries() {
        executeStatements(2);
    }

}