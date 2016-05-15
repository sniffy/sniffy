Using Sniffy Core API
============

Sniffy provides a convenient API for validating the number of executed database queries, affected database rows or even number of active TCP connections.
The main classes you should use are `io.sniffy.Sniffy` and `io.sniffy.Spy`

`Spy` objects are responsible for recording the executed queries and bytes sent over the wire. `Spy` stores all the information since the moment it was created.
`Sniffy` class provides convenient factory methods for creating `Spy` instances

```java
package io.sniffy.usage;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlQueries;
import io.sniffy.test.Count;
import io.sniffy.test.junit.SniffyRule;
import org.junit.Rule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class UsageTest {

    @Test
    public void testVerifyApi() throws SQLException {
        // Just add sniffy: in front of your JDBC connection URL in order to enable sniffer
        Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa");
        // Spy holds the amount of queries executed till the given amount of time
        // It acts as a base for further assertions
        Spy<?> spy = Sniffy.spy();
        // You do not need to modify your JDBC code
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        // spy.verify(SqlQueries.atMostOneQuery()) throws an AssertionError if more than one query was executed;
        spy.verify(SqlQueries.atMostOneQuery());
        // spy.verify(SqlQueries.noneQueries().otherThreads()) throws an AssertionError if at least one query was executed
        // by the thread other than then current one
        spy.verify(SqlQueries.noneQueries().otherThreads());
    }

    @Test
    public void testFunctionalApi() throws SQLException {
        // Just add sniffy: in front of your JDBC connection URL in order to enable sniffer
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa");
        // Sniffy.execute() method executes the lambda expression and returns an instance of Spy
        // which provides methods for validating the number of executed queries in given lambda
        Sniffy.execute(
                () -> connection.createStatement().execute("SELECT 1 FROM DUAL")
        ).verify(SqlQueries.atMostOneQuery());
    }

    @Test
    public void testResourceApi() throws SQLException {
        // Just add sniffy: in front of your JDBC connection URL in order to enable sniffer
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa");
        // You can use Sniffy in a try-with-resource block using expect methods instead of verify
        // When the try-with-resource block is completed, Sniffy will verify all the expectations defined
        try (@SuppressWarnings("unused") Spy s = Sniffy.
                expect(SqlQueries.atMostOneQuery()).
                expect(SqlQueries.noneQueries().otherThreads());
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 FROM DUAL");
        }
    }

    // Integrate Sniffy to your test using @Rule annotation and a SniffyRule field
    @Rule
    public final SniffyRule sniffyRule = new SniffyRule();

    // Now just add @Expectation or @Expectations annotations to define number of queries allowed for given method
    @Test
    @SqlExpectation(count = @Count(1))
    public void testJUnitIntegration() throws SQLException {
        // Just add sniffy: in front of your JDBC connection URL in order to enable sniffer
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa");
        // Do not make any changes in your code - just add the @Rule SniffyRule and put annotations on your test method
        connection.createStatement().execute("SELECT 1 FROM DUAL");
    }

}
```


