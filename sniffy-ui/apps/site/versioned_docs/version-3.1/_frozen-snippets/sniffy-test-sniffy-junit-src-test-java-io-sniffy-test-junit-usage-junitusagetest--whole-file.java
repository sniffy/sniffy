package io.sniffy.test.junit.usage;

import io.sniffy.socket.DisableSockets;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.test.Count;
import io.sniffy.test.junit.SniffyRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class JUnitUsageTest {

    @Rule
    public final SniffyRule sniffyRule = new SniffyRule(); // <1>

    @Test
    @SqlExpectation(count = @Count(1)) // <2>
    public void testJUnitIntegration() throws SQLException {
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <3>
        connection.createStatement().execute("SELECT 1 FROM DUAL"); // <4>
    }

    @Test
    @DisableSockets // <5>
    public void testDisableSockets() throws IOException {
        try {
            new Socket("google.com", 22); // <6>
            fail("Sniffy should have thrown ConnectException");
        } catch (ConnectException e) {
            assertNotNull(e);
        }
    }

}