package io.sniffy.tls;

import io.sniffy.Constants;
import io.sniffy.util.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SniffySSLContextProvider extends Provider {

    public static final String SNIFFY_PROVIDER_NAME = "Sniffy";

    public SniffySSLContextProvider() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchAlgorithmException, NoSuchFieldException {
        this(SNIFFY_PROVIDER_NAME, Constants.MAJOR_VERSION, "SniffySSLContextProvider");
    }

    public SniffySSLContextProvider(Provider delegate) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        super(delegate.getName(), delegate.getVersion(), delegate.getInfo());

        for (Service service : delegate.getServices()) {
            putServiceIfAbsent(new SniffySSLContextProviderService(
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

    public SniffySSLContextProvider(String providerName, double version, String providerInfo) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchAlgorithmException, NoSuchFieldException {
        super(providerName, version, providerInfo);

        for (Map.Entry<String, Service[]> entry : SniffyProviderListUtil.getOriginalSslContextProviders().entrySet()) {
            String algorithm = entry.getKey();
            for (Service service : entry.getValue()) {
                if (SNIFFY_PROVIDER_NAME.equals(getName()) || getName().equals(service.getProvider().getName())) {
                    putServiceIfAbsent(new SniffySSLContextProviderService(
                            this,
                            SniffyProviderListUtil.SSLCONTEXT,
                            algorithm,
                            SniffySSLContextProviderService.class.getName(),
                            new ArrayList<String>(),
                            extractAttributes(service),
                            service)
                    );
                }
            }
        }

    }

    private void putServiceIfAbsent(Service service) {
        if (null == getService(service.getAlgorithm(), service.getType())) {
            putService(service);
        }
    }

    public static Map<String, String> extractAttributes(Service service) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        Map<String, String> resultAttributes = new HashMap<String, String>();
        Map<?,String> attributes = ReflectionUtil.getField(Service.class, service, "attributes");
        for (Map.Entry<?, String> entry : attributes.entrySet()) {
            String key = ReflectionUtil.getField("java.security.Provider$UString", entry.getKey(), "string");
            String value = entry.getValue();
            resultAttributes.put(key, value);
        }
        return resultAttributes;
    }

}
