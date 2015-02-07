package com.github.bedrin.jdbc.sniffer.junit;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class QueryCounterTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public QueryCounter queryCounter = new QueryCounter();

    @BeforeClass
    public static void loadDriver() throws ClassNotFoundException {
        Class.forName("com.github.bedrin.jdbc.sniffer.MockDriver");
    }

    @Test
    @AllowedQueries(1)
    public void testAllowedOneQuery() throws SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
    }

    @Test
    @NotAllowedQueries
    public void testNotAllowedQueries() throws SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(1)
    public void testAllowedOneQueryExecutedTwo() throws SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(min = 1)
    public void testAllowedMinOneQueryExecutedTwo() throws SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
    }

    @Test
    @AllowedQueries(min = 2)
    public void testAllowedMinTwoQueriesExecutedOne() throws SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(exact = 2)
    public void testAllowedExactTwoQueriesExecutedTwo() throws SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
    }

    @Test
    @AllowedQueries(exact = 2)
    public void testAllowedExactTwoQueriesExecutedThree() throws SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        thrown.expect(AssertionError.class);
    }

    @Test
    @AllowedQueries(2)
    public void testAllowedTwoQueries() throws SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
    }

}