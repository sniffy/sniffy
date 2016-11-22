package io.sniffy.boot;

import io.sniffy.servlet.SnifferFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableSniffy(injectHtml = "@injectHtml")
@ContextConfiguration(classes = EnableSniffyTest.class)
public class EnableSniffyTest {

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public boolean injectHtml() {
        return true;
    }

    @Test
    public void testFilterBeanCreated() {
        SnifferFilter filter = applicationContext.getBean(SnifferFilter.class);
        assertNotNull(filter);
        assertTrue(filter.isEnabled());
    }

}
