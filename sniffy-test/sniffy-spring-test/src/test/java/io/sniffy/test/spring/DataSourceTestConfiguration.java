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

@Configuration
public class DataSourceTestConfiguration {

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
