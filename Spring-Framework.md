Integration with Spring Framework
====

Sniffy comes with a [Spring Framework](http://projects.spring.io/spring-framework/) via QueryCounterListener spring [@TestExecutionListener](http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#testcontext-tel-config)
Just add `@TestExecutionListeners(QueryCounterListener.class)` to your Spring test class and place appropriate expectations on your test methods like shown below:

```java
package io.sniffy.spring;

import io.sniffy.BaseTest;
import io.sniffy.Query;
import io.sniffy.Threads;
import io.sniffy.WrongNumberOfQueriesError;
import io.sniffy.Expectation;
import io.sniffy.Expectations;
import io.sniffy.NoQueriesAllowed;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@NoQueriesAllowed
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = QueryCounterTest.class)
@TestExecutionListeners(QueryCounterListener.class)
public class QueryCounterTest extends BaseTest {

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
```