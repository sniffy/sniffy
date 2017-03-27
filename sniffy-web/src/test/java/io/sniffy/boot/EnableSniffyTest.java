package io.sniffy.boot;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.Threads;
import io.sniffy.servlet.SniffyFilter;
import io.sniffy.sql.SniffyDataSource;
import io.sniffy.sql.SqlStatement;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.model.SeverityLevel;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableSniffy(
        injectHtml = "#{injectHtml}",
        filterEnabled = "${filterEnabled}",
        advanced = @SniffyAdvancedConfiguration(
                topSqlCapacity = "#{topSqlCapacity}",
                excludePattern = "#{excludePattern}",
                injectHtmlExcludePattern = "^/peds.html$"
        )
)
@ContextConfiguration(classes = EnableSniffyTest.class)
@PropertySource("classpath:/test.properties")
public class EnableSniffyTest {

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public boolean injectHtml() {
        return true;
    }

    @Bean
    public String excludePattern() {
        return "^/vets.html$";
    }

    @Bean
    public long topSqlCapacity() {
        return 42;
    }

    @Bean(name = "dataSource")
    public DataSource dataSource() throws SQLException {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        JdbcDataSource targetDataSource = Mockito.spy(h2DataSource);

        Mockito.when(targetDataSource.getConnection()).then(invocation -> {
            Sniffy.logSocket(1, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9876), 2, 3, 4);
            return invocation.callRealMethod();
        });

        return targetDataSource;
    }

    @Test
    public void testTopSqlCapacitySet() {
        assertEquals(topSqlCapacity(), ((ConcurrentLinkedHashMap) Sniffy.getGlobalSqlStats()).capacity());
    }

    @Test
    public void testExcludePatternSet() {
        assertEquals(excludePattern(), applicationContext.getBean(SniffyFilter.class).getExcludePattern().pattern());
    }

    @Test
    public void testInjectHtmlExcludePatternSet() {
        assertEquals("^/peds.html$", applicationContext.getBean(SniffyFilter.class).getInjectHtmlExcludePattern().pattern());
    }

    @Test
    public void testFilterBeanCreated() {
        SniffyFilter filter = applicationContext.getBean(SniffyFilter.class);
        assertNotNull(filter);
        assertTrue(filter.isEnabled());
    }

    @Test
    public void testDataSourceWrapped() {
        DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);
        assertTrue(dataSource instanceof SniffyDataSource);
    }

    @Test
    @Issue("issues/264")
    @Severity(SeverityLevel.NORMAL)
    public void testDataSourceWrapperWorks() throws Exception {
        DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);

        try (Spy<? extends Spy> spy = Sniffy.spy()) {

            @SuppressWarnings("unchecked") Callable<Connection> callableProxy = (Callable<Connection>) Proxy.newProxyInstance(
                    new URLClassLoader(new URL[0]),
                    new Class[]{Callable.class},
                    (proxy, method, args) -> dataSource.getConnection()
            );

            Connection connection = callableProxy.call();
            assertNotNull(connection);

            Map<StatementMetaData, SqlStats> executedStatements =
                    spy.getExecutedStatements(Threads.CURRENT, false);
            assertNotNull(executedStatements);
            assertEquals(1, executedStatements.size());
            StatementMetaData statementMetaData = executedStatements.keySet().iterator().next();
            assertNotNull(statementMetaData);
            assertNotNull(statementMetaData.stackTrace);
            assertEquals(SqlStatement.SYSTEM, statementMetaData.query);

        }

    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
