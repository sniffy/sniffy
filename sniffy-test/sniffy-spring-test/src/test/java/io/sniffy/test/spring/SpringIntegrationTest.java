package io.sniffy.test.spring;

import io.sniffy.*;
import io.sniffy.spring.QueryCounterListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringIntegrationTest.class)
@TestExecutionListeners(QueryCounterListener.class)
public class SpringIntegrationTest extends BaseTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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
    public void testWithoutExpectations() {
        assertTrue(true);
    }

    @Test
    @Expectation(atLeast = 2)
    public void testAllowedAtLeastTwoExecutedOne() {
        executeStatement();
        thrown.expect(WrongNumberOfQueriesError.class);
    }

}