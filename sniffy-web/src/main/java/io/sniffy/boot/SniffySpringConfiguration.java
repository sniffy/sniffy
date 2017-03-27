package io.sniffy.boot;

import io.sniffy.Sniffy;
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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import javax.servlet.DispatcherType;
import javax.sql.DataSource;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

    private Integer topSqlCapacity;

    private Boolean filterEnabled;
    private Pattern excludePattern;

    private Boolean injectHtml;
    private Pattern injectHtmlExcludePattern;


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

        SniffyConfiguration.INSTANCE.setMonitorJdbc(isMonitorJdbc());
        SniffyConfiguration.INSTANCE.setMonitorSocket(isMonitorSocket());
        SniffyConfiguration.INSTANCE.setTopSqlCapacity(getTopSqlCapacity());

        Sniffy.initialize();

        SniffyFilter sniffyFilter = new SniffyFilter();

        sniffyFilter.setFilterEnabled(isFilterEnabled());
        sniffyFilter.setExcludePattern(getExcludePattern());

        sniffyFilter.setInjectHtml(isInjectHtml());
        sniffyFilter.setInjectHtmlExcludePattern(getInjectHtmlExcludePattern());

        return sniffyFilter;
    }

    @Bean
    public FilterRegistrationBean sniffyFilterRegistration(SniffyFilter sniffyFilter) {

        FilterRegistrationBean filterRegistration = new FilterRegistrationBean(sniffyFilter);
        filterRegistration.setAsyncSupported(true);
        filterRegistration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));

        filterRegistration.setName("sniffyFilter");
        filterRegistration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        filterRegistration.setMatchAfter(false);

        filterRegistration.addUrlPatterns("/*");

        return filterRegistration;

    }

    private boolean isMonitorJdbc() {
        if (null == monitorJdbc) {
            monitorJdbc = resolveBooleanProperty(enableSniffy.getString("monitorJdbc"));
        }
        return monitorJdbc;
    }

    private boolean isMonitorSocket() {
        if (null == monitorSocket) {
            monitorSocket = resolveBooleanProperty(enableSniffy.getString("monitorSocket"));
        }
        return monitorSocket;
    }

    private int getTopSqlCapacity() {
        if (null == topSqlCapacity) {
            topSqlCapacity = resolveIntProperty(
                    enableSniffy.getAnnotation("advanced").getString("topSqlCapacity")
            );
        }
        return topSqlCapacity;
    }

    private boolean isInjectHtml() {
        if (null == injectHtml) {
            injectHtml = resolveBooleanProperty(enableSniffy.getString("injectHtml"));
        }
        return injectHtml;
    }

    public Pattern getInjectHtmlExcludePattern() {
        if (null == injectHtmlExcludePattern) {

            try {
                String injectHtmlExcludePattern = resolveStringProperty(
                        enableSniffy.getAnnotation("advanced").getString("injectHtmlExcludePattern")
                );
                if (!injectHtmlExcludePattern.isEmpty()) {
                    this.injectHtmlExcludePattern = Pattern.compile(injectHtmlExcludePattern);
                }
            } catch (PatternSyntaxException e) {
                // TODO: log me maybe?
            }

        }
        return injectHtmlExcludePattern;
    }

    private boolean isFilterEnabled() {
        if (null == filterEnabled) {
            filterEnabled = resolveBooleanProperty(enableSniffy.getString("filterEnabled"));
        }
        return filterEnabled;
    }

    public Pattern getExcludePattern() {
        if (null == excludePattern) {

            try {
                String excludePattern = resolveStringProperty(
                        enableSniffy.getAnnotation("advanced").getString("excludePattern")
                );
                if (!excludePattern.isEmpty()) {
                    this.excludePattern = Pattern.compile(excludePattern);
                }
            } catch (PatternSyntaxException e) {
                // TODO: log me maybe?
            }

        }
        return excludePattern;
    }

    private boolean resolveBooleanProperty(String attributeValue) {
        Boolean value;

        String resolvedValue = beanFactory.resolveEmbeddedValue(attributeValue);
        Object injectHtmlObj = resolver.evaluate(resolvedValue, expressionContext);
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

    private int resolveIntProperty(String attributeValue) {
        int value;

        String resolvedValue = beanFactory.resolveEmbeddedValue(attributeValue);
        Object injectHtmlObj = resolver.evaluate(resolvedValue, expressionContext);
        try {
            value = typeConverter.convertIfNecessary(injectHtmlObj, Integer.class);
        } catch (TypeMismatchException e) {
            value = 0;
        }

        return value;
    }

    private String resolveStringProperty(String attributeValue) {
        String value;

        String resolvedValue = beanFactory.resolveEmbeddedValue(attributeValue);
        Object injectHtmlObj = resolver.evaluate(resolvedValue, expressionContext);
        try {
            value = typeConverter.convertIfNecessary(injectHtmlObj, String.class);
        } catch (TypeMismatchException e) {
            value = null;
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