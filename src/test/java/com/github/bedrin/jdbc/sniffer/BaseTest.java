package com.github.bedrin.jdbc.sniffer;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseTest {

    /**
     * H2 is keeping the schema while we have at least one connection
     */
    private static Connection keepAlive;

    @BeforeClass
    public static void loadDriverAndCreateTables() throws ClassNotFoundException, SQLException {
        Class.forName("com.github.bedrin.jdbc.sniffer.MockDriver");
        keepAlive = DriverManager.getConnection("sniffer:jdbc:h2:mem:project", "sa", "sa");
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:project", "sa", "sa");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS PROJECT (ID NUMBER PRIMARY KEY, NAME VARCHAR(255))");
            statement.execute("CREATE SEQUENCE IF NOT EXISTS SEQ_PROJECT");
        }
    }

    @AfterClass
    public static void closeKeepAliveConnection() throws SQLException {
        keepAlive.close();
    }

    protected static void executeStatement() {
        executeStatements(1, Query.SELECT);
    }

    protected static void executeStatement(Query query) {
        executeStatements(1, query);
    }

    protected static void executeStatements(int count) {
        executeStatements(count, Query.SELECT);
    }

    protected static void executeStatements(int count, Query query) {
        try {
            try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:project", "sa", "sa");
                 Statement statement = connection.createStatement()) {
                for (int i = 0; i < count; i++) {
                    switch (query) {
                        case INSERT:
                            statement.executeUpdate("INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (SEQ_PROJECT.NEXTVAL, 'foo')");
                            break;
                        case UPDATE:
                            statement.executeUpdate("UPDATE PUBLIC.PROJECT SET NAME = UPPER(NAME)");
                            break;
                        case DELETE:
                            statement.executeUpdate("DELETE FROM PUBLIC.PROJECT");
                            break;
                        case MERGE:
                            statement.executeUpdate("MERGE INTO PUBLIC.PROJECT (ID, NAME) KEY (ID) VALUES (SEQ_PROJECT.NEXTVAL, 'bar')");
                            break;
                        case OTHER:
                            statement.execute("CREATE TABLE IF NOT EXISTS PROJECT (ID NUMBER PRIMARY KEY, NAME VARCHAR(255))");
                            break;
                        case SELECT:
                        case ANY:
                        default:
                            statement.execute("SELECT 1 FROM DUAL");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void executeStatementInOtherThread() {
        executeStatementsInOtherThread(1);
    }

    protected static void executeStatementInOtherThread(Query query) {
        executeStatementsInOtherThread(1, query);
    }

    protected static void executeStatementsInOtherThread(int count) {
        executeStatementsInOtherThread(count, Query.SELECT);
    }
    protected static void executeStatementsInOtherThread(int count, Query query) {
        Thread thread = new Thread(() -> {BaseTest.executeStatements(count, query);});
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
