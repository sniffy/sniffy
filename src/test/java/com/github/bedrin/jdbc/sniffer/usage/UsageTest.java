package com.github.bedrin.jdbc.sniffer.usage;

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
