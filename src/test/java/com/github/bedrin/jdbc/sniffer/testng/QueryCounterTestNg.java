package com.github.bedrin.jdbc.sniffer.testng;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import com.github.bedrin.jdbc.sniffer.Query;
import com.github.bedrin.jdbc.sniffer.Threads;
import com.github.bedrin.jdbc.sniffer.junit.Expectation;
import com.github.bedrin.jdbc.sniffer.junit.Expectations;
import com.github.bedrin.jdbc.sniffer.junit.NoQueriesAllowed;
import org.testng.annotations.*;

import java.sql.SQLException;

@Listeners({QueryCounter.class, MustFailListener.class})
@NoQueriesAllowed
public class QueryCounterTestNg extends BaseTest {

    @BeforeClass
    @Expectation(atLeast = 0) // TODO: introduce @AnyQueriesAllowed annotation
    public void setUp() throws ClassNotFoundException, SQLException {
        BaseTest.loadDriverAndCreateTables();
    }

    @Test
    @MustFail
    public void testNotAllowedQueriesByDefault() {
        executeStatement();
    }

    @Test
    @Expectation(1)
    public void testAllowedOneQuery() {
        executeStatement();
    }

    @Expectation(value = 5, atLeast = 2)
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testAmbiguousExpectationAnnotation() {
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    @Expectation(2)
    @NoQueriesAllowed
    public void testAmbiguousAnnotations() {
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    @Expectations({
            @Expectation(2),
            @Expectation(value = 2, threads = Threads.OTHERS)
    })
    @NoQueriesAllowed
    public void testAmbiguousAnnotations2() {
    }

    @Test
    @NoQueriesAllowed
    @MustFail
    public void testNotAllowedQueries() {
        executeStatement();
    }

    @Test
    @Expectation(1)
    @MustFail
    public void testAllowedOneQueryExecutedTwo() {
        executeStatements(2);
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
    @MustFail
    public void testAllowedMinTwoQueriesExecutedOne() {
        executeStatement();
    }

    @Test
    @Expectation(value = 2)
    public void testAllowedExactTwoQueriesExecutedTwo() {
        executeStatements(2);
    }

    @Test
    @Expectation(value = 2)
    @MustFail
    public void testAllowedExactTwoQueriesExecutedThree() {
        executeStatements(3);
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

    @Test
    @Expectations({
            @Expectation(value = 1, query = Query.SELECT),
            @Expectation(value = 1, query = Query.INSERT),
            @Expectation(value = 1, query = Query.UPDATE),
            @Expectation(value = 1, query = Query.DELETE),
            @Expectation(value = 1, query = Query.MERGE)
    })
    public void testDifferentQueries() {
        executeStatement(Query.SELECT);
        executeStatement(Query.INSERT);
        executeStatement(Query.UPDATE);
        executeStatement(Query.DELETE);
        executeStatement(Query.MERGE);
    }

}
