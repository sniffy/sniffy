Integration with JUnit
====

JDBC Sniffer comes with a [JUnit](http://junit.org/) @Rule for quick integration with test framework. Just add `@Rule public final QueryCounter queryCounter = new QueryCounter();` to your TestNG test class and place appropriate expectations on your test methods like shown below:

```java
package com.github.bedrin.jdbc.sniffer.usage;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import com.github.bedrin.jdbc.sniffer.Threads;
import com.github.bedrin.jdbc.sniffer.junit.Expectation;
import com.github.bedrin.jdbc.sniffer.junit.Expectations;
import com.github.bedrin.jdbc.sniffer.junit.QueryCounter;
import org.junit.Rule;
import org.junit.Test;

public class QueryCounterTest extends BaseTest {

    // Integrate JDBC Sniffer to your test using @Rule annotation and a QueryCounter field
    @Rule
    public final QueryCounter queryCounter = new QueryCounter();

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