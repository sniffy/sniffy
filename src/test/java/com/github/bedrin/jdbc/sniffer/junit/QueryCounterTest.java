package com.github.bedrin.jdbc.sniffer.junit;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import com.github.bedrin.jdbc.sniffer.WrongNumberOfQueriesError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class QueryCounterTest extends BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public QueryCounter queryCounter = new QueryCounter();

    @Test
    @ExpectedQueries(1)
    public void testAllowedOneQuery() {
        executeStatement();
    }

    @Test
    @NoQueriesAllowed
    public void testNotAllowedQueries() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @ExpectedQueries(1)
    public void testAllowedOneQueryExecutedTwo() {
        executeStatements(2);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @ExpectedQueries(atLeast = 1)
    public void testAllowedMinOneQueryExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @ExpectedQueries(atLeast = 2)
    public void testAllowedMinTwoQueriesExecutedOne() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @ExpectedQueries(value = 2)
    public void testAllowedExactTwoQueriesExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @ExpectedQueries(value = 2)
    public void testAllowedExactTwoQueriesExecutedThree() {
        executeStatements(3);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @ExpectedQueries(2)
    public void testAllowedTwoQueries() {
        executeStatements(2);
    }

}