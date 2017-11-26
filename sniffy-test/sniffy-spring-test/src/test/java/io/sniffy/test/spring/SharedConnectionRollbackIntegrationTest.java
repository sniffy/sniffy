package io.sniffy.test.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceTestConfiguration.class, SharedConnectionRollbackIntegrationTest.class})
@TestExecutionListeners(value = SniffySpringTestListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@EnableSharedConnection
@Transactional
@Rollback
public class SharedConnectionRollbackIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @SharedConnection
    public void testSharedConnectionTwoRows() throws SQLException, ExecutionException, InterruptedException {

        jdbcTemplate.batchUpdate(
                "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (SEQ_PROJECT.NEXTVAL, ?)",
                Arrays.asList(new Object[]{"foo"}, new Object[]{"bar"})
        );

        assertEquals(2, newSingleThreadExecutor().submit(
                () -> jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PUBLIC.PROJECT", Integer.class)
        ).get().intValue());

    }

    @Test
    @SharedConnection
    public void testSharedConnectionThreeRows() throws SQLException, ExecutionException, InterruptedException {

        jdbcTemplate.batchUpdate(
                "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (SEQ_PROJECT.NEXTVAL, ?)",
                Arrays.asList(new Object[]{"foo"}, new Object[]{"bar"}, new Object[]{"baz"})
        );

        assertEquals(3, newSingleThreadExecutor().submit(
                () -> jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PUBLIC.PROJECT", Integer.class)
        ).get().intValue());

    }

}
