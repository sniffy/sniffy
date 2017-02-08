package io.sniffy.test.junit.usage;
// tag::JUnitUsage[]
import io.sniffy.sql.SqlExpectation;
import io.sniffy.test.Count;
import io.sniffy.test.junit.SniffyRule;
import org.junit.Rule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JUnitUsageTest {

    @Rule
    public final SniffyRule sniffyRule = new SniffyRule(); // <1>

    @Test
    @SqlExpectation(count = @Count(1)) // <2>
    public void testJUnitIntegration() throws SQLException {
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <3>
        connection.createStatement().execute("SELECT 1 FROM DUAL"); // <4>
    }

}
// end::JUnitUsage[]