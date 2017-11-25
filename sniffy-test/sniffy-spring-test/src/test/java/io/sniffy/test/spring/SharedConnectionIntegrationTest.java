package io.sniffy.test.spring;

import io.sniffy.BaseTest;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void testSharedConnection() throws SQLException, ExecutionException, InterruptedException {

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (SEQ_PROJECT.NEXTVAL, ?)"
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
