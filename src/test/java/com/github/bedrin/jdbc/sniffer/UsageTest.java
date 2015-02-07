package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.Assert.*;

public class UsageTest {

    @Test
    public void testExecuteStatement() throws ClassNotFoundException, SQLException {
        // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        // Sniffer.reset() sets the internal counter of queries to zero
        Sniffer.reset();
        // You do not need to modify your JDBC code
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        // Sniffer.executedStatements() returns count of execute queries
        assertEquals(1, Sniffer.executedStatements());
        // Sniffer.verifyNotMoreThanOne() throws an AssertionError if more than one query was executed;
        // it also resets the counter to 0
        Sniffer.verifyNotMoreThanOne();
        // Sniffer.verifyNotMore() throws an AssertionError if any query was executed
        Sniffer.verifyNotMore();
    }

    @Test
    public void testFunctionalApi() throws SQLException {
        final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        // Sniffer.execute() method executes the lambda expression and returns an instance of RecordedQueries
        // this class provides methods for validating the number of executed queries
        Sniffer.execute(() -> connection.createStatement().execute("SELECT 1 FROM DUAL")).verifyNotMoreThanOne();
    }

}
