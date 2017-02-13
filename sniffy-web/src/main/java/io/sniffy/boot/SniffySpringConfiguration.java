package io.sniffy.boot;

import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.servlet.SniffyFilter;
import io.sniffy.sql.SniffyDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
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

/**
 * @since 3.1
 */
@Configuration
public class SniffySpringConfiguration implements ImportAware, BeanFactoryAware, BeanPostProcessor {

    private ConfigurableBeanFactory beanFactory;
    private BeanExpressionResolver resolver;
    private BeanExpressionContext expressionContext;
    private TypeConverter typeConverter;

    private AnnotationAttributes enableSniffy;

    private Boolean monitorJdbc;
    private Boolean monitorSocket;

    private Boolean filterEnabled;
    private Boolean injectHtml;

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
    public SniffyFilter sniffyFilter() {

        SniffyConfiguration.INSTANCE.setMonitorSocket(isMonitorSocket());

        SniffyFilter sniffyFilter = new SniffyFilter();
        sniffyFilter.setEnabled(isFilterEnabled());
        sniffyFilter.setInjectHtml(isInjectHtml());

        return sniffyFilter;
    }

    private boolean isMonitorJdbc() {
        if (null == monitorJdbc) {
            monitorJdbc = resolveBooleanProperty("monitorJdbc");
        }
        return monitorJdbc;
    }

    private boolean isMonitorSocket() {
        if (null == monitorSocket) {
            monitorSocket = resolveBooleanProperty("monitorSocket");
        }
        return monitorSocket;
    }


    private boolean isInjectHtml() {
        if (null == injectHtml) {
            injectHtml = resolveBooleanProperty("injectHtml");
        }
        return injectHtml;
    }

    private boolean isFilterEnabled() {
        if (null == filterEnabled) {
            filterEnabled = resolveBooleanProperty("filterEnabled");
        }
        return filterEnabled;
    }

    private boolean resolveBooleanProperty(String attributeName) {
        Boolean value;

        String injectHtmlAttribute = enableSniffy.getString(attributeName);
        injectHtmlAttribute = beanFactory.resolveEmbeddedValue(injectHtmlAttribute);
        Object injectHtmlObj = resolver.evaluate(injectHtmlAttribute, expressionContext);
        try {
            value = typeConverter.convertIfNecessary(injectHtmlObj, Boolean.class);
        } catch (TypeMismatchException e) {
            value = false;
        }

        if (null == value) {
            value = false;
        }

        return value;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (isMonitorJdbc() && bean instanceof DataSource && !(bean instanceof SniffyDataSource)) {
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