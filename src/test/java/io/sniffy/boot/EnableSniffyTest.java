package io.sniffy.boot;

import io.sniffy.servlet.SnifferFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableSniffy
@ContextConfiguration(classes = EnableSniffyTest.class)
public class EnableSniffyTest {

    @Resource
    private ApplicationContext applicationContext;

    @Test
    public void testFilterBeanCreated() {
        assertNotNull(applicationContext.getBean(SnifferFilter.class));
    }

}
