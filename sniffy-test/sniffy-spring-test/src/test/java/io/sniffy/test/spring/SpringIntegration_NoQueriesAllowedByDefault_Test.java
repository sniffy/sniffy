package io.sniffy.test.spring;

import io.sniffy.*;
import io.sniffy.sql.NoSql;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@NoSql
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringIntegration_NoQueriesAllowedByDefault_Test.class)
@TestExecutionListeners(SniffySpringTestListener.class)
public class SpringIntegration_NoQueriesAllowedByDefault_Test extends BaseTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testNotAllowedQueriesByDefault() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    public void testSuppressedException() {
        thrown.expect(RuntimeException.class);
        executeStatement();
        throw new RuntimeException();
    }

    @Test
    @Expectations({
            @Expectation(atMost = 1, threads = Threads.CURRENT),
            @Expectation(atMost = 1, threads = Threads.OTHERS, query = Query.DELETE),
    })
    public void testExpectations() {
        executeStatement();
        executeStatementInOtherThread(Query.DELETE);
    }

    @Test
    @Expectation(1)
    public void testAllowedOneQueryExecutedTwo() {
        executeStatements(2);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

    @Test
    @Expectation(value = 1, query = Query.SELECT)
    public void testAllowedSelectExecutedUpdate() {
        executeStatement(Query.UPDATE);
        thrown.expect(WrongNumberOfQueriesError.class);
    }

}