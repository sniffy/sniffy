package io.sniffy.tls;

import io.sniffy.util.ReflectionUtil;
import io.sniffy.util.StackTraceExtractor;

import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SniffySSLContextSpiProvider extends Provider {

    private final Provider originalProvider;

    public SniffySSLContextSpiProvider(Provider delegate) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        this(delegate, delegate.getName(), delegate.getVersion(), delegate.getInfo());
    }

    public SniffySSLContextSpiProvider(Provider delegate, String providerName, double providerVersion, String providerInfo) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        super(providerName, providerVersion, providerInfo);

        this.originalProvider = delegate;

        for (Service service : delegate.getServices()) {
            putServiceIfAbsent(new SniffySSLContextSpiProviderService(
                    this,
                    service.getType(),
                    service.getAlgorithm(),
                    service.getClassName(),
                    new ArrayList<String>(),
                    extractAttributes(service),
                    service)
            );
        }
    }

    @Override
    public String getName() {
        String providerName = super.getName();
        if ("Sniffy-SunJSSE".equals(providerName) && StackTraceExtractor.hasClassAndMethodInStackTrace(
                "sun.security.ssl.SSLContextImpl", "getTrustManagers")) {
            providerName = "SunJSSE";
        }
        return providerName;
    }

    private void putServiceIfAbsent(Service service) {
        if (null == getService(service.getAlgorithm(), service.getType())) {
            putService(service);
        }
    }

    public static Map<String, String> extractAttributes(Service service) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        Map<String, String> resultAttributes = new HashMap<String, String>();
        Map<?, String> attributes = ReflectionUtil.getField(Service.class, service, "attributes");
        for (Map.Entry<?, String> entry : attributes.entrySet()) {
            String key = ReflectionUtil.getField("java.security.Provider$UString", entry.getKey(), "string");
            String value = entry.getValue();
            resultAttributes.put(key, value);
        }
        return resultAttributes;
    }

    public Provider getOriginalProvider() {
        return originalProvider;
    }

}
