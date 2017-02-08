package io.sniffy.test.junit;

import io.sniffy.WrongNumberOfQueriesError;
import io.sniffy.junit.QueryCounter;
import io.sniffy.test.BaseNoQueriesAllowedTest;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class InheritSuperClassAnnotationTest extends BaseNoQueriesAllowedTest {

    @BeforeClass
    public static void loadDriver() throws ClassNotFoundException {
        Class.forName("io.sniffy.MockDriver");
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public QueryCounter queryCounter = new QueryCounter();

    @Test
    public void testNoQueriesAllowedBySuperTest() {
        try {
            try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        thrown.expect(WrongNumberOfQueriesError.class);
    }

}
