package io.sniffy.test.junit.usage;

import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlStatement;
import io.sniffy.sql.WrongNumberOfQueriesError;
import io.sniffy.test.Count;
import io.sniffy.test.junit.SniffyRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class JUnitOverviewTest {
    // tag::JUnitOverview[]
    @Rule public SniffyRule sniffyRule = new SniffyRule();

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    @SqlExpectation(count = @Count(1))
    public void testExpectedOneQueryGotOne() throws SQLException {
        DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa").createStatement().execute("SELECT 1 FROM DUAL"); // <4>
    }

    @Test
    @SqlExpectation(count = @Count(max = 1), query = SqlStatement.SELECT)
    public void testExpectedNotMoreThanOneSelectGotTwo() throws SQLException {
        try (Statement statement = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa").createStatement()) {
            statement.execute("SELECT 1 FROM DUAL");
            statement.execute("SELECT 2 FROM DUAL");
        }
        thrown.expect(WrongNumberOfQueriesError.class);
    }
    // end::JUnitOverview[]
}
