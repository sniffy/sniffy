package io.sniffy.boot;

import io.sniffy.servlet.SniffyFilter;
import io.sniffy.sql.SniffyDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import javax.sql.DataSource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableSniffy(injectHtml = "#{injectHtml}", filterEnabled = "${filterEnabled}")
@ContextConfiguration(classes = EnableSniffyTest.class)
@PropertySource("classpath:/test.properties")
public class EnableSniffyTest {

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public boolean injectHtml() {
        return true;
    }

    @Bean(name = "dataSource")
    public DataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:");
        return dataSource;
    }

    @Test
    public void testFilterBeanCreated() {
        SniffyFilter filter = applicationContext.getBean(SniffyFilter.class);
        assertNotNull(filter);
        assertTrue(filter.isEnabled());

        DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);
        assertTrue(dataSource instanceof SniffyDataSource);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
