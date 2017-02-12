package io.sniffy.test.spring.usage;

import io.sniffy.sql.SqlExpectation;
import io.sniffy.test.Count;
import io.sniffy.test.spring.SniffySpringTestListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringUsageTest.class)
@TestExecutionListeners(SniffySpringTestListener.class) // <1>
public class SpringUsageTest {

    @Test
    @SqlExpectation(count = @Count(1)) // <2>
    public void testJUnitIntegration() throws SQLException {
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <3>
        connection.createStatement().execute("SELECT 1 FROM DUAL"); // <4>
    }

}
