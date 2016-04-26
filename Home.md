Sniffy
============

[![Join the chat at https://gitter.im/sniffy/sniffy](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/sniffy/sniffy?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/ec48f442755f4df5b62bf3bcba3a2246)](https://www.codacy.com/app/sniffy/sniffy)
[![CI Status](https://travis-ci.org/sniffy/sniffy.svg?branch=master)](https://travis-ci.org/sniffy/sniffy)
[![Coverage Status](https://coveralls.io/repos/sniffy/sniffy/badge.png?branch=master)](https://coveralls.io/r/sniffy/sniffy?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.sniffy/sniffy/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.sniffy/sniffy)
[![Download](https://api.bintray.com/packages/sniffy/sniffy/sniffy/images/download.svg) ](https://bintray.com/sniffy/sniffy/sniffy/_latestVersion)
[![License](http://img.shields.io/:license-mit-blue.svg?style=flat)](http://badges.mit-license.org)

Sniffy is a lightweight low-overhead Java profiler which shows the results directly in your browser
![RecordedDemo](http://sniffy.io/demo.gif)

Live Demo - [http://demo.sniffy.io/](http://demo.sniffy.io/owners.html?lastName=)

In order to enable Sniffy simply add it to your `web.xml` file:
```xml
<filter>
    <filter-name>sniffer</filter-name>
    <filter-class>io.sniffy.servlet.SnifferFilter</filter-class>
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

Sniffy also allows you to check if particular method doesn't make more than N SQL queries in your unit tests
Especially it's useful to catch the ORM [N+1 problem](http://stackoverflow.com/questions/97197/what-is-the-n1-selects-issue) at early stages 

```java
try (Spy s = Sniffer.expectAtMostOnce(Query.SELECT).expectNever(Threads.OTHERS);
     Statement statement = connection.createStatement()) {
    statement.execute("SELECT 1 FROM DUAL");
    // Sniffy will throw an Exception if you execute query other than SELECT or uncomment line below
    //statement.execute("SELECT 1 FROM DUAL");
}
```

Maven
============
Sniffy is available from Maven Central repository
```xml
<dependency>
    <groupId>io.sniffy</groupId>
    <artifactId>sniffy</artifactId>
    <version>3.0.7</version>
</dependency>
```

For Gradle users:
```javascript
dependencies {
    compile 'io.sniffy:sniffy:3.0.7'
}
```

Download
============
[![Get automatic notifications about new "sniffy" versions](https://www.bintray.com/docs/images/bintray_badge_color.png) ](https://bintray.com/sniffy/sniffy/sniffy/view?source=watch)
- [sniffy-3.0.7.jar](https://github.com/sniffy/sniffy/releases/download/3.0.7/sniffy-3.0.7.jar) ([bintray mirror](https://bintray.com/artifact/download/sniffy/sniffy/sniffy-3.0.7.jar))
- [sniffy-3.0.7-sources.jar](https://github.com/sniffy/sniffy/releases/download/3.0.7/sniffy-3.0.7-sources.jar) ([bintray mirror](https://bintray.com/artifact/download/sniffy/sniffy/sniffy-3.0.7-sources.jar))
- [sniffy-3.0.7-javadoc.jar](https://github.com/sniffy/sniffy/releases/download/3.0.7/sniffy-3.0.7-javadoc.jar) ([bintray mirror](https://bintray.com/artifact/download/sniffy/sniffy/sniffy-3.0.7-javadoc.jar))

Setup
============
Simply add sniffy.jar to your classpath and add `sniffer:` prefix to the JDBC connection url
For example `jdbc:h2:~/test` should be changed to `sniffer:jdbc:h2:mem:`
The Sniffy JDBC driver class name is `io.sniffy.MockDriver`

HTML injection is configured in `web.xml` file:
```xml
<filter>
    <filter-name>sniffer</filter-name>
    <filter-class>io.sniffy.servlet.SnifferFilter</filter-class>
    <init-param>
        <!-- 
        Enables injection of Sniffy toolbar to HTML
        If disabled the html remains untouched
        You still can get the number of executed queries from X-Sql-Queries HTTP header
         -->
        <param-name>inject-html</param-name>
        <param-value>true</param-value> <!-- default: false -->
    </init-param>
    <init-param>
        <!-- Allows disabling the Sniffy filter in web.xml -->
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

Or if you are using [Spring Boot](http://projects.spring.io/spring-boot/), simply add `@EnableSniffy` to your application class.
You still need to modify the JDBC settings in order to intercept SQL queries:
```yml
spring:
  datasource:
    url: sniffer:jdbc:mysql://188.166.164.153:3306/dolbot
    driver-class-name: io.sniffy.MockDriver
```

Using Sniffy in unit tests
============
Following test shows the main ways of integrating Sniffy into your unit tests:

```java
import io.sniffy.Sniffer;
import io.sniffy.Spy;
import io.sniffy.Threads;
import io.sniffy.Expectation;
import io.sniffy.junit.QueryCounter;
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

    // Integrate Sniffy to your test using @Rule annotation and a QueryCounter field
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
Sniffy provides integration with popular testing frameworks - see our wiki for details
 
 * [JUnit](https://github.com/sniffy/sniffy/wiki/JUnit)
 * [Spring Framework](https://github.com/sniffy/sniffy/wiki/Spring-Framework)
 * [Spock Framework](https://github.com/sniffy/sniffy/wiki/Spock-Framework)
 * [Test NG](https://github.com/sniffy/sniffy/wiki/Test-NG)

Building
============
JDBC sniffer is built using JDK8+ and Maven 3.2+ - just checkout the project and type `mvn install`
JDK8 is required only for building the project - once it's built, you can use Sniffy with any JRE 1.5+

UI part of Sniffy is maintained in a separate repository [sniffy-ui](https://github.com/sniffy/sniffy-ui)

Contribute
============
You are most welcome to contribute to Sniffy!

Read the [Contribution guidelines](https://github.com/sniffy/sniffy/blob/master/CONTRIBUTING.md)
