package io.sniffy.test.junit.jupiter.compat;

import io.sniffy.sql.SqlExpectation;
import io.sniffy.test.Count;
import io.sniffy.test.junit.jupiter.SniffyExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@ExtendWith(SniffyExtension.class)
public class JUnit6SameArtifactCompatibilityTest {

    @Test
    @SqlExpectation(count = @Count(1))
    public void executesWithJUnit6AgainstPublishedJupiterArtifact() throws Exception {
        Class.forName("org.h2.Driver");
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
        try {
            Statement statement = connection.createStatement();
            try {
                statement.execute("SELECT 1");
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }
}
