package io.sniffy.test.spring;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

@Configuration
public class DataSourceTestConfiguration {

    @Bean public Object something() {
        Thread t = new Thread(() -> {

            try {

                Thread.sleep(5000);

                Thread.getAllStackTraces().entrySet().stream()
                        .filter(entry -> !entry.getKey().isDaemon())
                        .filter(entry -> entry.getKey().isAlive())
                        .forEach(entry -> {
                            System.out.println(entry.getKey().getName() + " " + entry.getKey().getState());
                            System.out.println(Arrays.toString(entry.getValue()));
                            System.out.println();
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        t.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(t);
        return new Object();
    }

    @Bean
    public DataSource originalDataSource() throws SQLException {
        JdbcDataSource jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:springtests;autocommit=off");
        return jdbcDataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate() throws SQLException {
        DataSource dataSource = originalDataSource();
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public DataSourceTransactionManager dataSourceTransactionManager() throws SQLException {
        return new DataSourceTransactionManager(originalDataSource());
    }

    @Bean(destroyMethod = "close")
    public Connection keepAliveConnection(DataSource originalDataSource) throws SQLException {
        Connection keepAliveConnection = originalDataSource.getConnection();
        try (Connection connection = originalDataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS PROJECT (ID NUMBER PRIMARY KEY, NAME VARCHAR(255))");
            statement.execute("CREATE SEQUENCE IF NOT EXISTS SEQ_PROJECT");
        }
        return keepAliveConnection;
    }

}
