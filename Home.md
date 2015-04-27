JDBC Sniffer
============
[![CI Status](https://travis-ci.org/bedrin/jdbc-sniffer.svg?branch=master)](https://travis-ci.org/bedrin/jdbc-sniffer)
[![Coverage Status](https://coveralls.io/repos/bedrin/jdbc-sniffer/badge.png?branch=master)](https://coveralls.io/r/bedrin/jdbc-sniffer?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.bedrin/jdbc-sniffer/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.bedrin/jdbc-sniffer)
[![Download](https://api.bintray.com/packages/bedrin/github/jdbc-sniffer/images/download.svg) ](https://bintray.com/bedrin/github/jdbc-sniffer/_latestVersion)

JDBC Sniffer counts the number of executed SQL queries and provides an API for validating it
It is very useful in unit tests and allows you to test if particular method doesn't make more than N SQL queries

```java
try (Spy s = Sniffer.expectAtMostOnce().expectNever(Threads.OTHERS);
     Statement statement = connection.createStatement()) {
    statement.execute("SELECT 1 FROM DUAL");
}
```

Maven
============
JDBC Sniffer is available from Maven Central repository
```xml
<dependency>
    <groupId>com.github.bedrin</groupId>
    <artifactId>jdbc-sniffer</artifactId>
    <version>2.1</version>
</dependency>
```

For Gradle users:
```javascript
dependencies {
    compile 'com.github.bedrin:jdbc-sniffer:2.1'
}
```

Download
============
- [jdbc-sniffer-2.1.jar](https://github.com/bedrin/jdbc-sniffer/releases/download/2.1/jdbc-sniffer-2.1.jar)
- [jdbc-sniffer-2.1-sources.jar](https://github.com/bedrin/jdbc-sniffer/releases/download/2.1/jdbc-sniffer-2.1-sources.jar)
- [jdbc-sniffer-2.1-javadoc.jar](https://github.com/bedrin/jdbc-sniffer/releases/download/2.1/jdbc-sniffer-2.1-javadoc.jar)

Setup
============
Simply add jdbc-sniffer.jar to your classpath and add `sniffer:` prefix to the JDBC connection url
For example `jdbc:h2:~/test` should be changed to `sniffer:jdbc:h2:mem:`
The sniffer JDBC driver class name is `com.github.bedrin.jdbc.sniffer.MockDriver`

Usage
============
Following test shows all ways of integrating JDBC Sniffer into your project:

```java
import com.github.bedrin.jdbc.sniffer.Sniffer;
import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.Threads;
import com.github.bedrin.jdbc.sniffer.junit.Expectation;
import com.github.bedrin.jdbc.sniffer.junit.QueryCounter;
import org.junit.Rule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class UsageTest {

    @Test
    public void testVerifyApi() throws SQLException {
        // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
        // Spy holds the amount of queries executed till the given amount of time
        // It acts as a base for further assertions
        Spy spy = Sniffer.spy();
        // You do not need to modify your JDBC code
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        assertEquals(1, spy.executedStatements());
        // Sniffer.verifyAtMostOnce() throws an AssertionError if more than one query was executed;
        spy.verifyAtMostOnce();
        // Sniffer.verifyNever(Threads.OTHERS) throws an AssertionError if at least one query was executed
        // by the thread other than then current one
        spy.verifyNever(Threads.OTHERS);
    }

    @Test
    public void testFunctionalApi() throws SQLException {
        // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
        final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
        // Sniffer.execute() method executes the lambda expression and returns an instance of Spy
        // which provides methods for validating the number of executed queries in given lambda
        Sniffer.execute(() -> connection.createStatement().execute("SELECT 1 FROM DUAL")).verifyAtMostOnce();
    }

    @Test
    public void testResourceApi() throws SQLException {
        // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
        final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
        // You can use Sniffer in a try-with-resource block using expect methods instead of verify
        // When the try-with-resource block is completed, JDBC Sniffer will verify all the expectations defined
        try (@SuppressWarnings("unused") Spy s = Sniffer.expectAtMostOnce().expectNever(Threads.OTHERS);
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 FROM DUAL");
        }
    }

    // Integrate JDBC Sniffer to your test using @Rule annotation and a QueryCounter field
    @Rule
    public final QueryCounter queryCounter = new QueryCounter();

    // Now just add @Expectation or @Expectations annotations to define number of queries allowed for given method
    @Test
    @Expectation(1)
    public void testJUnitIntegration() throws SQLException {
        // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
        final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
        // Do not make any changes in your code - just add the @Rule QueryCounter and put annotations on your test method
        connection.createStatement().execute("SELECT 1 FROM DUAL");
    }

}
```

Building
============
JDBC sniffer is built using JDK8+ and Maven 3+ - just checkout the project and type `mvn install`
JDK8 is required only for building the project - once it's built, you can use JDBC Sniffer with JRE 1.5+

Contribute
============
You are most welcome to contribute to JDBC Sniffer!

Read the [Contribution guidelines](https://github.com/bedrin/jdbc-sniffer/blob/master/CONTRIBUTING.md)
