package io.sniffy.test.spring.usage;

import io.sniffy.test.spring.DataSourceTestConfiguration;
import io.sniffy.test.spring.EnableSharedConnection;
import io.sniffy.test.spring.SharedConnection;
import io.sniffy.test.spring.SniffySpringTestListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.qatools.allure.annotations.Features;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceTestConfiguration.class, SpringSharedConnectionUsageTest.class})
@TestExecutionListeners(value = SniffySpringTestListener.class, mergeMode = MERGE_WITH_DEFAULTS) // <1>
@EnableSharedConnection // <2>
@Transactional
@Rollback
public class SpringSharedConnectionUsageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @SharedConnection // <3>
    @Features("issue/344")
    public void testSharedConnectionTwoRows() throws SQLException, ExecutionException, InterruptedException {

        jdbcTemplate.batchUpdate(
                "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (NEXT VALUE FOR SEQ_PROJECT, ?)",
                Arrays.asList(new Object[]{"foo"}, new Object[]{"bar"})
        ); // <4>

        assertEquals(2, newSingleThreadExecutor().submit(
                () -> jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PUBLIC.PROJECT", Integer.class) // <5>
        ).get().intValue());

    }

}
