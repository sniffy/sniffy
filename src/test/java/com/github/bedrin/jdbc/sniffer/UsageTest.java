package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class UsageTest {

    @Test
    public void testExecuteStatement() throws ClassNotFoundException, SQLException {
        // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        // Sniffer.spy() // TODO add description
        Spy spy = Sniffer.spy();
        // You do not need to modify your JDBC code
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        // Sniffer.executedStatements() returns count of execute queries
        assertEquals(1, spy.executedStatements());
        // Sniffer.verifyAtMostOnce() throws an AssertionError if more than one query was executed;
        // it also resets the counter to 0
        spy.verifyAtMostOnce();
    }

    @Test
    public void testFunctionalApi() throws SQLException {
        final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        // Sniffer.execute() method executes the lambda expression and returns an instance of RecordedQueries
        // this class provides methods for validating the number of executed queries
        Sniffer.execute(() -> connection.createStatement().execute("SELECT 1 FROM DUAL")).verifyAtMostOnce();
    }

    @Test
    public void testTryWithResourceApi() throws SQLException {
        final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        try (@SuppressWarnings("unused") Spy s = Sniffer.expectNotMoreThanOne();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 FROM DUAL");
        }
    }

}
