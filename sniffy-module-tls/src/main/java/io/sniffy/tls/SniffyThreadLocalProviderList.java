package io.sniffy.tls;

import io.sniffy.Constants;
import io.sniffy.util.ReflectionUtil;
import io.sniffy.util.StackTraceExtractor;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import java.security.Provider;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.sniffy.tls.SniffySecurityUtil.SSLCONTEXT;

class SniffyThreadLocalProviderList extends ThreadLocal<ProviderList> {

    private final ThreadLocal<ProviderList> delegate = new ThreadLocal<ProviderList>();

    private final ThreadLocal<Boolean> insideSetProviderList = new ThreadLocal<Boolean>();

    @Override
    public ProviderList get() {

        ProviderList providerList = delegate.get();

        if (null == providerList) {
            if (!Boolean.TRUE.equals(insideSetProviderList.get()) && StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "setProviderList")) {
                insideSetProviderList.set(true);
                return ProviderList.newList();
            }
        }

        return providerList;
    }


    private static Map.Entry<ProviderList, SniffySSLContextSpiProvider> wrapProviderList(ProviderList value) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {

        List<Provider> wrappedProviderList = new ArrayList<Provider>();

        SniffySSLContextSpiProvider firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi = null;

        List<Provider> originalSecurityProviders = value.providers();

        for (Provider originalSecurityProvider : originalSecurityProviders) {

            if (!(originalSecurityProvider instanceof SniffySSLContextSpiProvider)) {

                boolean hasSSLContextService = false;
                boolean hasDefaultSSLContextService = false;

                for (Provider.Service service : originalSecurityProvider.getServices()) {
                    if (SSLCONTEXT.equals(service.getType())) {
                        hasSSLContextService = true;
                        if ("Default".equalsIgnoreCase(service.getAlgorithm())) {
                            hasDefaultSSLContextService = true;
                            break;
                        }
                    }
                }

                if (hasSSLContextService) {
                    String originalProviderName = originalSecurityProvider.getName();

                    SniffySSLContextSpiProvider sniffySSLContextSpiProvider = new SniffySSLContextSpiProvider(originalSecurityProvider);

                    if (hasDefaultSSLContextService && null == firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi) {
                        firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi = sniffySSLContextSpiProvider;
                    }

                    wrappedProviderList.add(new SniffySSLContextSpiProvider(
                            originalSecurityProvider,
                            "Sniffy-" + originalProviderName,
                            Constants.MAJOR_VERSION,
                            "SniffySSLContextProvider"
                    ));
                    wrappedProviderList.add(sniffySSLContextSpiProvider);

                } else {
                    wrappedProviderList.add(originalSecurityProvider);
                }

            } else {
                wrappedProviderList.add(originalSecurityProvider);
            }

        }

        return new AbstractMap.SimpleImmutableEntry<ProviderList, SniffySSLContextSpiProvider>(
                ProviderList.newList(wrappedProviderList.toArray(new Provider[0])),
                firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi
        );

    }

    @Override
    public void set(ProviderList value) {

        if (null == delegate.get() && Boolean.TRUE.equals(insideSetProviderList.get())) {
            if (StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "setProviderList")) {
                // call callback
                try {

                    Map.Entry<ProviderList, SniffySSLContextSpiProvider> tuple = wrapProviderList(value);
                    value = tuple.getKey();

                    ReflectionUtil.invokeMethod(Providers.class, null, "setSystemProviderList", ProviderList.class, value, Void.class);

                    SniffySSLContextSpiProvider firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi = tuple.getValue();

                    if (null != firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi) {
                        Provider.Service defaultSniffySSLContextSpiProviderService =
                                firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi.getService(SSLCONTEXT, "Default");
                        if (null != defaultSniffySSLContextSpiProviderService) {
                            SSLContext.setDefault(
                                    new SniffySSLContext(
                                            (SSLContextSpi) defaultSniffySSLContextSpiProviderService.newInstance(null),
                                            firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi,
                                            "Default"
                                    )
                            );
                        }
                    }

                    insideSetProviderList.set(false);
                    return;

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            insideSetProviderList.set(false);
        }

        delegate.set(value);
    }

    @Override
    public void remove() {
        delegate.remove();
    }

}
