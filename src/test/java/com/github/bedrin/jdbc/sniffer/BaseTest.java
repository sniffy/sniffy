package com.github.bedrin.jdbc.sniffer;

import org.junit.BeforeClass;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseTest {

    @BeforeClass
    public static void loadDriver() throws ClassNotFoundException {
        Class.forName("com.github.bedrin.jdbc.sniffer.MockDriver");
    }

    protected static void executeStatement() {
        executeStatements(1);
    }

    protected static void executeStatements(int count) {
        try {
            try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
                 Statement statement = connection.createStatement()) {
                for (int i = 0; i < count; i++) {
                    statement.execute("SELECT 1 FROM DUAL");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void executeStatementInOtherThread() {
        executeStatementsInOtherThread(1);
    }

    protected static void executeStatementsInOtherThread(int count) {
        Thread thread = new Thread(() -> {BaseTest.executeStatements(count);});
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
