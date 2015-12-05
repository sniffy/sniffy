Integration with Test NG
====

Sniffy comes with a [Test NG](http://testng.org/) listener for quick integration with test framework. Just add `@Listeners(QueryCounter.class)` to your TestNG test class and place appropriate expectations on your test methods like shown below:

```java
package io.sniffy.testng;

import io.sniffy.BaseTest;
import io.sniffy.Threads;
import io.sniffy.Expectation;
import io.sniffy.Expectations;
import io.sniffy.NoQueriesAllowed;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(QueryCounter.class)
@NoQueriesAllowed
public class QueryCounterTest extends BaseTest {

    @Test
    @Expectations({
            @Expectation(atMost = 1, threads = Threads.CURRENT),
            @Expectation(atMost = 1, threads = Threads.OTHERS),
    })
    public void testExpectations() {
        executeStatement();
        executeStatementInOtherThread();
    }

}
```