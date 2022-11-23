package io.sniffy.tls;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.UnsafeInvocationException;
import io.sniffy.util.StackTraceExtractor;

import java.security.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.sniffy.reflection.Unsafe.$;

public class SniffySSLContextSpiProvider extends Provider {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySSLContextSpiProvider.class);

    private final Provider originalProvider;

    public SniffySSLContextSpiProvider(Provider delegate) throws UnresolvedRefException, UnsafeInvocationException {
        this(delegate, delegate.getName(), delegate.getVersion(), delegate.getInfo());
    }

    public SniffySSLContextSpiProvider(Provider delegate, String providerName, double providerVersion, String providerInfo) throws UnresolvedRefException, UnsafeInvocationException {
        super(providerName, providerVersion, providerInfo);
        LOG.trace("Created SniffySSLContextSpiProvider(" + delegate + ", " + providerName + ", " + providerVersion + ", " + providerInfo + ")");

        this.originalProvider = delegate;

        for (Service service : delegate.getServices()) {
            putServiceIfAbsent(new SniffySSLContextSpiProviderService(
                    this,
                    service.getType(),
                    service.getAlgorithm(),
                    service.getClassName(),
                    extractAliases(service),
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
            LOG.trace("Mocking SunJSSE provider name since Sniffy was called from sun.security.ssl.SSLContextImpl.getTrustManagers()");
            providerName = "SunJSSE";
        }
        return providerName;
    }

    private void putServiceIfAbsent(Service service) {
        if (null == getService(service.getAlgorithm(), service.getType())) {
            putService(service);
        }
    }

    public static List<String> extractAliases(Service service) throws UnresolvedRefException, UnsafeInvocationException {
        return $(Service.class).<List<String>>getNonStaticField("aliases").get(service);
    }

    public static Map<String, String> extractAttributes(Service service) throws UnresolvedRefException, UnsafeInvocationException {
        Map<String, String> resultAttributes = new HashMap<String, String>();
        Map<?, String> attributes = $(Service.class).<Map<?, String>>getNonStaticField("attributes").get(service);
        for (Map.Entry<?, String> entry : attributes.entrySet()) {
            String key = $("java.security.Provider$UString").<String>tryGetNonStaticField("string").get(entry.getKey());
            String value = entry.getValue();
            resultAttributes.put(key, value);
        }
        return resultAttributes;
    }

    public Provider getOriginalProvider() {
        return originalProvider;
    }

}
