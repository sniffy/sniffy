Integration with Test NG
====

JDBC Sniffer comes with a [Test NG](http://testng.org/) listener for quick integration with test framework. Just add `@Listeners(QueryCounter.class)` to your TestNG test class and place appropriate expectations on your test methods like shown below:

```java
package com.github.bedrin.jdbc.sniffer.testng;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import com.github.bedrin.jdbc.sniffer.Threads;
import com.github.bedrin.jdbc.sniffer.junit.Expectation;
import com.github.bedrin.jdbc.sniffer.junit.Expectations;
import com.github.bedrin.jdbc.sniffer.junit.NoQueriesAllowed;
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