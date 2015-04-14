package com.github.bedrin.jdbc.sniffer.junit;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import com.github.bedrin.jdbc.sniffer.Threads;
import com.github.bedrin.jdbc.sniffer.WrongNumberOfQueriesError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@NoQueriesAllowed
public class QueryCounterTest extends BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public QueryCounter queryCounter = new QueryCounter();

    @Test
    public void testNotAllowedQueriesByDefault() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(1)
    public void testAllowedOneQuery() {
        executeStatement();
    }

    @Test
    @Expectation(value = 5, atLeast = 2)
    public void testAmbiguousExpectationAnnotation() {
        thrown.expect(IllegalArgumentException.class);
    }

    @Test
    @Expectation(2)
    @NoQueriesAllowed
    public void testAmbiguousAnnotations() {
        thrown.expect(IllegalArgumentException.class);
    }

    @Test
    @Expectations({
            @Expectation(2),
            @Expectation(value = 2, threads = Threads.OTHERS)
    })
    @NoQueriesAllowed
    public void testAmbiguousAnnotations2() {
        thrown.expect(IllegalArgumentException.class);
    }

    @Test
    @NoQueriesAllowed
    public void testNotAllowedQueries() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(1)
    public void testAllowedOneQueryExecutedTwo() {
        executeStatements(2);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectations({
            @Expectation(atMost = 1, threads = Threads.CURRENT),
            @Expectation(atMost = 1, threads = Threads.OTHERS),
    })
    public void testExpectations() {
        executeStatement();
        executeStatementInOtherThread();
    }

    @Test
    @Expectation(atLeast = 1)
    public void testAllowedMinOneQueryExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @Expectation(atLeast = 2)
    public void testAllowedMinTwoQueriesExecutedOne() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(value = 2)
    public void testAllowedExactTwoQueriesExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @Expectation(value = 2)
    public void testAllowedExactTwoQueriesExecutedThree() {
        executeStatements(3);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(2)
    public void testAllowedTwoQueries() {
        executeStatements(2);
    }

    @Test
    @Expectation(atLeast = 1, atMost = 3)
    public void testBetween() {
        executeStatements(2);
    }

}