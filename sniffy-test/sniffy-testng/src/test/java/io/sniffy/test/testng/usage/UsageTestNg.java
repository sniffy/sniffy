package io.sniffy.test.testng.usage;

import io.sniffy.sql.SqlExpectation;
import io.sniffy.test.Count;
import io.sniffy.test.testng.SniffyTestNgListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Listeners(SniffyTestNgListener.class) // <1>
public class UsageTestNg {

    @Test
    @SqlExpectation(count = @Count(1)) // <2>
    public void testJUnitIntegration() throws SQLException {
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <3>
        connection.createStatement().execute("SELECT 1 FROM DUAL"); // <4>
    }

}
