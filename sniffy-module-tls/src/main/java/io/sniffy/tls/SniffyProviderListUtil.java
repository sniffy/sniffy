package io.sniffy.tls;

import io.sniffy.util.ReflectionUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.*;

import static io.sniffy.tls.SniffySSLContextProvider.SNIFFY_PROVIDER_NAME;

public class SniffyProviderListUtil {

    public static final String SSLCONTEXT = "SSLContext";

    private static volatile Map<String, Provider.Service[]> ORIGINAL_SSL_CONTEXT_PROVIDERS;

    private static volatile SSLContextSpi ORIGINAL_SSL_CONTEXT_SPI;

    public static Map<String, Provider.Service[]> getOriginalSslContextProviders() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchAlgorithmException {
        if (null == ORIGINAL_SSL_CONTEXT_PROVIDERS) {
            synchronized (SniffyProviderListUtil.class) {
                if (null == ORIGINAL_SSL_CONTEXT_PROVIDERS) {
                    Map<String, List<Provider.Service>> sslContextProviders = getOriginalSSLContextProvidersImpl();
                    ORIGINAL_SSL_CONTEXT_PROVIDERS = new LinkedHashMap<String, Provider.Service[]>(sslContextProviders.size());
                    for (Map.Entry<String, List<Provider.Service>> entry : sslContextProviders.entrySet()) {
                        if (null == ORIGINAL_SSL_CONTEXT_SPI && "Default".equalsIgnoreCase(entry.getKey()) && !entry.getValue().isEmpty()) {
                            ORIGINAL_SSL_CONTEXT_SPI = (SSLContextSpi) entry.getValue().get(0).newInstance(null);
                        }
                        ORIGINAL_SSL_CONTEXT_PROVIDERS.put(entry.getKey(), entry.getValue().toArray(new Provider.Service[0]));
                    }
                    ORIGINAL_SSL_CONTEXT_PROVIDERS = Collections.unmodifiableMap(ORIGINAL_SSL_CONTEXT_PROVIDERS);
                }
            }
        }
        return ORIGINAL_SSL_CONTEXT_PROVIDERS;
    }

    public static SSLContextSpi getOriginalSslContextSpi() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchAlgorithmException {
        if (null == ORIGINAL_SSL_CONTEXT_SPI) {
            synchronized (SniffyProviderListUtil.class) {
                if (null == ORIGINAL_SSL_CONTEXT_SPI) {
                    getOriginalSslContextProviders();
                }
            }
        }
        return ORIGINAL_SSL_CONTEXT_SPI;
    }

    private static Set<Provider> getOriginalJsseProviders() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchAlgorithmException {
        Set<Provider> originalProviders = new HashSet<Provider>();

        for (Provider.Service[] services : getOriginalSslContextProviders().values()) {
            for (Provider.Service service: services) {
                originalProviders.add(service.getProvider());
            }
        }
        return originalProviders;
    }

    private static Map<String, List<Provider.Service>> getOriginalSSLContextProvidersImpl() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Map<String, List<Provider.Service>> sslContextProviders = new LinkedHashMap<String, List<Provider.Service>>();

        for (Provider provider : Security.getProviders()) {
            for (Provider.Service service : provider.getServices()) {
                if (SSLCONTEXT.equals(service.getType())) {
                    List<Provider.Service> providers = sslContextProviders.get(service.getAlgorithm());
                    //noinspection Java8MapApi
                    if (null == providers) {
                        providers = new ArrayList<Provider.Service>();
                        sslContextProviders.put(service.getAlgorithm(), providers);
                    }
                    providers.add(service);
                }
            }
        }
        return sslContextProviders;
    }

    private static class SniffySSLContextChild extends SSLContext {

        public SniffySSLContextChild(SSLContextSpi contextSpi, Provider provider, String protocol) {
            super(contextSpi, provider, protocol);
        }

    }

    public static void install() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchAlgorithmException, NoSuchFieldException {

        SniffySSLContextProvider sniffySSLContextProvider = new SniffySSLContextProvider();
        Security.insertProviderAt(sniffySSLContextProvider, 1);

        SSLContext.setDefault(new SniffySSLContextChild(new SniffySSLContext(getOriginalSslContextSpi()), sniffySSLContextProvider, "Default"));

        Set<Provider> originalProviders = getOriginalJsseProviders();

        for (Provider provider : originalProviders) {
            Security.removeProvider(provider.getName());
            Security.addProvider(new SniffySSLContextProvider(provider));
        }

    }

    public static void uninstall() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        Class<?> providersClass = Class.forName("sun.security.jca.Providers");
        Class<?> providerListClass = Class.forName("sun.security.jca.ProviderList");

        Object list = ReflectionUtil.invokeMethod(providersClass, null, "getProviderList");
        Object providerList = ReflectionUtil.invokeMethod(providerListClass, null, "remove",
                providerListClass, list,
                String.class, SNIFFY_PROVIDER_NAME
        );
        ReflectionUtil.invokeMethod(providersClass, null, "setProviderList", providerListClass, providerList);

    }

}
