package io.sniffy.junit.usage;

import io.sniffy.sql.SqlExpectation;
import io.sniffy.test.Count;
import io.sniffy.test.junit.SniffyRule;
import org.junit.Rule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class UsageTest {

    // Integrate Sniffy to your test using @Rule annotation and a SniffyRule field
    @Rule
    public final SniffyRule sniffyRule = new SniffyRule();

    // Now just add @Expectation or @Expectations annotations to define number of queries allowed for given method
    @Test
    @SqlExpectation(count = @Count(1))
    public void testJUnitIntegration() throws SQLException {
        // Just add sniffy: in front of your JDBC connection URL in order to enable sniffer
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa");
        // Do not make any changes in your code - just add the @Rule SniffyRule and put annotations on your test method
        connection.createStatement().execute("SELECT 1 FROM DUAL");
    }

}
