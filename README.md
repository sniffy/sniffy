JDBC Sniffer
============

[![Join the chat at https://gitter.im/bedrin/jdbc-sniffer](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/bedrin/jdbc-sniffer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![CI Status](https://travis-ci.org/bedrin/jdbc-sniffer.svg?branch=master)](https://travis-ci.org/bedrin/jdbc-sniffer)
[![Coverage Status](https://coveralls.io/repos/bedrin/jdbc-sniffer/badge.png?branch=master)](https://coveralls.io/r/bedrin/jdbc-sniffer?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.bedrin/jdbc-sniffer/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.bedrin/jdbc-sniffer)
[![Download](https://api.bintray.com/packages/bedrin/github/jdbc-sniffer/images/download.svg) ](https://bintray.com/bedrin/github/jdbc-sniffer/_latestVersion)

JDBC Sniffer counts the number of executed SQL queries and provides an API for validating them
It is designed for unit tests and allows you to test if particular method doesn't make more than N SQL queries
Especially it's useful to catch the ORM [N+1 problem](http://stackoverflow.com/questions/97197/what-is-the-n1-selects-issue) at early stages 

```java
try (Spy s = Sniffer.expectAtMostOnce(Query.SELECT).expectNever(Threads.OTHERS);
     Statement statement = connection.createStatement()) {
    statement.execute("SELECT 1 FROM DUAL");
    // JDBC Sniffer will throw an Exception if you execute query other than SELECT or uncomment line below
    //statement.execute("SELECT 1 FROM DUAL");
}
```

You can also use JDBC Sniffer in your test environments to see the number of SQL queries executed by each HTTP request.
Just add it to your `web.xml` file:
```xml
<filter>
    <filter-name>sniffer</filter-name>
    <filter-class>com.github.bedrin.jdbc.sniffer.servlet.SnifferFilter</filter-class>
    <init-param>
        <param-name>inject-html</param-name>
        <param-value>true</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>sniffer</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

Restart your server and you will see the number of queries in bottom right corner of your app:

![SnifferFilterInjectHtml](https://bedrin.github.io/jdbc-sniffer/SnifferFilterInjectHtml.png)

Click on the icon to see the actual queries and their elapsed time:

![SnifferFilterViewQueries.png](https://bedrin.github.io/jdbc-sniffer/SnifferFilterViewQueries.png)

Maven
============
JDBC Sniffer is available from Maven Central repository
```xml
<dependency>
    <groupId>com.github.bedrin</groupId>
    <artifactId>jdbc-sniffer</artifactId>
    <version>2.3.2</version>
</dependency>
```

For Gradle users:
```javascript
dependencies {
    compile 'com.github.bedrin:jdbc-sniffer:2.3.2'
}
```

Download
============
[![Get automatic notifications about new "jdbc-sniffer" versions](https://www.bintray.com/docs/images/bintray_badge_color.png) ](https://bintray.com/bedrin/github/jdbc-sniffer/view?source=watch)
- [jdbc-sniffer-2.3.2.jar](https://github.com/bedrin/jdbc-sniffer/releases/download/2.3.2/jdbc-sniffer-2.3.2.jar) ([bintray mirror](https://bintray.com/artifact/download/bedrin/github/jdbc-sniffer-2.3.2.jar))
- [jdbc-sniffer-2.3.2-sources.jar](https://github.com/bedrin/jdbc-sniffer/releases/download/2.3.2/jdbc-sniffer-2.3.2-sources.jar) ([bintray mirror](https://bintray.com/artifact/download/bedrin/github/jdbc-sniffer-2.3.2-sources.jar))
- [jdbc-sniffer-2.3.2-javadoc.jar](https://github.com/bedrin/jdbc-sniffer/releases/download/2.3.2/jdbc-sniffer-2.3.2-javadoc.jar) ([bintray mirror](https://bintray.com/artifact/download/bedrin/github/jdbc-sniffer-2.3.2-javadoc.jar))

Setup
============
Simply add jdbc-sniffer.jar to your classpath and add `sniffer:` prefix to the JDBC connection url
For example `jdbc:h2:~/test` should be changed to `sniffer:jdbc:h2:mem:`
The sniffer JDBC driver class name is `com.github.bedrin.jdbc.sniffer.MockDriver`

HTML injection is configured in `web.xml` file:
```xml
<filter>
    <filter-name>sniffer</filter-name>
    <filter-class>com.github.bedrin.jdbc.sniffer.servlet.SnifferFilter</filter-class>
    <init-param>
        <!-- 
        Enables injection of JDBC Sniffer toolbar to HTML
        If disabled the html remains untouched
        You still can get the number of executed queries from X-Sql-Queries HTTP header
         -->
        <param-name>inject-html</param-name>
        <param-value>true</param-value> <!-- default: false -->
    </init-param>
    <init-param>
        <!-- Allows disabling the JDBC Sniffer filter in web.xml -->
        <param-name>enabled</param-name>
        <param-value>true</param-value> <!-- default: true -->
    </init-param>
    <init-param>
        <!-- Allows excluding some of the request URL's from Sniffer filter -->
        <param-name>exclude-pattern</param-name>
        <param-value>^/vets.html$</param-value> <!-- optional -->
    </init-param>
</filter>
<filter-mapping>
    <filter-name>sniffer</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

Usage
============
Following test shows the main ways of integrating JDBC Sniffer into your project:

```java
import com.github.bedrin.jdbc.sniffer.Sniffer;
import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.Threads;
import com.github.bedrin.jdbc.sniffer.Expectation;
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

Integrating with test frameworks
============
JDBC Sniffer provides integration with popular testing frameworks - see our wiki for details
 
 * [JUnit](https://github.com/bedrin/jdbc-sniffer/wiki/JUnit)
 * [Spring Framework](https://github.com/bedrin/jdbc-sniffer/wiki/Spring-Framework)
 * [Spock Framework](https://github.com/bedrin/jdbc-sniffer/wiki/Spock-Framework)
 * [Test NG](https://github.com/bedrin/jdbc-sniffer/wiki/Test-NG)

Building
============
JDBC sniffer is built using JDK8+ and Maven 3.2+ - just checkout the project and type `mvn install`
JDK8 is required only for building the project - once it's built, you can use JDBC Sniffer with any JRE 1.5+

Contribute
============
You are most welcome to contribute to JDBC Sniffer!

Read the [Contribution guidelines](https://github.com/bedrin/jdbc-sniffer/blob/master/CONTRIBUTING.md)
