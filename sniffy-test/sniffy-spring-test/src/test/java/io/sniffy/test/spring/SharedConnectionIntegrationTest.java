package io.sniffy.test.spring;

import io.qameta.allure.Feature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceTestConfiguration.class, SharedConnectionIntegrationTest.class})
@TestExecutionListeners(value = SniffySpringTestListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@EnableSharedConnection
public class SharedConnectionIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @SharedConnection
    @Feature("issue/344")
    public void testSharedConnection() throws SQLException, ExecutionException, InterruptedException {

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (NEXT VALUE FOR SEQ_PROJECT, ?)"
             )) {

            ps.setString(1, "foo");
            assertEquals(1, ps.executeUpdate());

            ps.setString(1, "bar");
            assertEquals(1, ps.executeUpdate());

            assertEquals(2, newSingleThreadExecutor().submit(() -> {

                int count = 0;

                try (Connection slaveConnection = dataSource.getConnection();
                     Statement statement = slaveConnection.createStatement();
                     ResultSet resultSet = statement.executeQuery("SELECT * FROM PUBLIC.PROJECT")) {

                    while (resultSet.next()) {
                        count++;
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return count;

            }).get().intValue());

        }

    }

}
