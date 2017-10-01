package io.sniffy.test.spring.usage;

import io.sniffy.socket.DisableSockets;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.test.Count;
import io.sniffy.test.spring.SniffySpringTestListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

import static java.sql.DriverManager.getConnection;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringUsageTest.class)
@TestExecutionListeners(SniffySpringTestListener.class) // <1>
public class SpringUsageTest {

    @Test
    @SqlExpectation(count = @Count(max = 1))
    public void testJUnitIntegration() throws SQLException {
        final Connection connection = getConnection(
                "sniffy:jdbc:h2:mem:", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
    }

    @Test
    @DisableSockets // <5>
    public void testDisableSockets() throws IOException {
        try {
            new Socket("google.com", 443); // <6>
            fail("Sniffy should have thrown ConnectException");
        } catch (ConnectException e) {
            assertNotNull(e);
        }
    }

}
