package io.sniffy.test.spring;

import io.sniffy.Sniffy;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * @since 3.1.13
 */
@Configuration
public class DisableSocketsGloballyConfiguration implements ImportAware, BeanFactoryAware, BeanPostProcessor {

    @Bean
    public ConnectionsRegistry connectionsRegistry() {

        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setMonitorJdbc(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setJdbcFaultInjectionEnabled(true);
        SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(true);

        Sniffy.initialize();

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, -1);

        return ConnectionsRegistry.INSTANCE;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        @SuppressWarnings("unused")
        Map<String, Object> map = importMetadata.getAnnotationAttributes(DisableSocketsGlobally.class.getName());
        connectionsRegistry();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        connectionsRegistry();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        connectionsRegistry();
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        connectionsRegistry();
        return bean;
    }

}
