package com.github.bedrin.jdbc.sniffer.testng;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import com.github.bedrin.jdbc.sniffer.junit.BasedNoQueriesAllowedTest;
import com.github.bedrin.jdbc.sniffer.junit.Expectation;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Listeners({QueryCounter.class, MustFailListener.class})
public class InheritSuperClassAnnotationTestNg extends BasedNoQueriesAllowedTest {

    @BeforeClass
    @Expectation(atLeast = 0)
    public void setUp() throws ClassNotFoundException, SQLException {
        BaseTest.loadDriverAndCreateTables();
    }

    @Test
    @MustFail
    public void testNoQueriesAllowedBySuperTest() {
        try {
            try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}