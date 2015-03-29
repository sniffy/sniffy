package com.github.bedrin.jdbc.sniffer.junit;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class QueryCounterTest extends BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public QueryCounter queryCounter = new QueryCounter();

    @Test
    @AllowedQueries(1)
    public void testAllowedOneQuery() {
        executeStatement();
    }

    @Test
    @NotAllowedQueries
    public void testNotAllowedQueries() {
        executeStatement();
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(1)
    public void testAllowedOneQueryExecutedTwo() {
        executeStatements(2);
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(min = 1)
    public void testAllowedMinOneQueryExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @AllowedQueries(min = 2)
    public void testAllowedMinTwoQueriesExecutedOne() {
        executeStatement();
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(exact = 2)
    public void testAllowedExactTwoQueriesExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @AllowedQueries(exact = 2)
    public void testAllowedExactTwoQueriesExecutedThree() {
        executeStatements(3);
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(2)
    public void testAllowedTwoQueries() {
        executeStatements(2);
    }

}