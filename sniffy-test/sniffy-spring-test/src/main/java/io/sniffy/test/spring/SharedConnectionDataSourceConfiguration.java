package io.sniffy.test.spring;

import io.sniffy.test.SharedConnectionDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @since 3.1.6
 */
@Configuration
public class SharedConnectionDataSourceConfiguration implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof SharedConnectionDataSource)) {
            return new SharedConnectionDataSource(DataSource.class.cast(bean));
        } else {
            return bean;
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
