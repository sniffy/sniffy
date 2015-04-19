package com.github.bedrin.jdbc.sniffer.testng;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import com.github.bedrin.jdbc.sniffer.junit.BasedNoQueriesAllowedTest;
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
    public void setUp() throws ClassNotFoundException {
        BaseTest.loadDriver();
    }

    @Test
    @MustFail
    public void testNoQueriesAllowedBySuperTest() {
        try {
            try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}