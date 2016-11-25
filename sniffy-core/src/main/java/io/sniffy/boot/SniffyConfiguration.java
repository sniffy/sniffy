package io.sniffy.boot;

import io.sniffy.servlet.SnifferFilter;
import io.sniffy.sql.SniffyDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class SniffyConfiguration implements ImportAware, BeanFactoryAware, BeanPostProcessor {

    private ConfigurableBeanFactory beanFactory;
    private BeanExpressionResolver resolver;
    private BeanExpressionContext expressionContext;
    private TypeConverter typeConverter;

    private AnnotationAttributes enableSniffy;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableSniffy.class.getName());
        this.enableSniffy = AnnotationAttributes.fromMap(map);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableBeanFactory) beanFactory;
        resolver = this.beanFactory.getBeanExpressionResolver();
        expressionContext = new BeanExpressionContext(this.beanFactory, null);
        typeConverter = this.beanFactory.getTypeConverter();
    }

    @Bean
    public SnifferFilter sniffyFilter() {

        SnifferFilter snifferFilter = new SnifferFilter();

        String enabled = enableSniffy.getString("enabled");
        enabled = beanFactory.resolveEmbeddedValue(enabled);
        Object enabledObj = resolver.evaluate(enabled, expressionContext);

        String injectHtml = enableSniffy.getString("injectHtml");
        injectHtml = beanFactory.resolveEmbeddedValue(injectHtml);
        Object injectHtmlObj = resolver.evaluate(injectHtml, expressionContext);

        snifferFilter.setEnabled(typeConverter.convertIfNecessary(enabledObj, Boolean.class));
        snifferFilter.setInjectHtml(typeConverter.convertIfNecessary(injectHtmlObj, Boolean.class));

        return snifferFilter;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        // TODO: check enabled property

        if (bean instanceof DataSource && !(bean instanceof SniffyDataSource)) {
            return SniffyDataSource.wrap(DataSource.class.cast(bean));
        } else {
            return bean;
        }

    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}