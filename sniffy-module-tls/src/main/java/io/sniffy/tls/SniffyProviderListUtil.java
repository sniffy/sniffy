package io.sniffy.tls;

import io.sniffy.util.ReflectionUtil;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;

import java.lang.reflect.InvocationTargetException;
import java.security.Provider;
import java.util.*;

import static io.sniffy.tls.SniffySSLContextProvider.SNIFFY_PROVIDER_NAME;

public class SniffyProviderListUtil {

    public static final String SSLCONTEXT = "SSLContext";

    private static volatile Map<String, Provider.Service[]> ORIGINAL_SSL_CONTEXT_PROVIDERS;

    public static Map<String, Provider.Service[]> getOriginalSslContextProviders() {
        if (null == ORIGINAL_SSL_CONTEXT_PROVIDERS) {
            synchronized (SniffyProviderListUtil.class) {
                if (null == ORIGINAL_SSL_CONTEXT_PROVIDERS) {
                    Map<String, List<Provider.Service>> sslContextProviders = getOriginalSSLContextProvidersImpl();
                    ORIGINAL_SSL_CONTEXT_PROVIDERS = new HashMap<String, Provider.Service[]>(sslContextProviders.size());
                    for (Map.Entry<String, List<Provider.Service>> entry : sslContextProviders.entrySet()) {
                        ORIGINAL_SSL_CONTEXT_PROVIDERS.put(entry.getKey(), entry.getValue().toArray(new Provider.Service[0]));
                    }
                    ORIGINAL_SSL_CONTEXT_PROVIDERS = Collections.unmodifiableMap(ORIGINAL_SSL_CONTEXT_PROVIDERS);
                }
            }
        }
        return ORIGINAL_SSL_CONTEXT_PROVIDERS;
    }

    private static Map<String, List<Provider.Service>> getOriginalSSLContextProvidersImpl() {
        Map<String, List<Provider.Service>> sslContextProviders = new HashMap<String, List<Provider.Service>>();
        ProviderList list = Providers.getProviderList();
        for (Provider provider : list.providers()) {
            for (Provider.Service service : provider.getServices()) {
                //System.out.println(service.getType() + " - " + service.getAlgorithm() + " = " + service.getClassName());
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



    public static void install() {

        ProviderList list = Providers.getProviderList();
        ProviderList providerList = ProviderList.insertAt(list, new SniffySSLContextProvider(), 0);
        Providers.setProviderList(providerList);

    }

    public static void uninstall() {

        ProviderList list = Providers.getProviderList();
        Provider sniffyProvider = list.getProvider(SNIFFY_PROVIDER_NAME);
        if (null != sniffyProvider) {
            ProviderList providerList = ProviderList.remove(list, SNIFFY_PROVIDER_NAME);
            Providers.setProviderList(providerList);
        }

    }

    public static void wrapSSLContextServiceProvidersWithSniffy() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        // TODO: incorporate to install-uninstall methods

        getOriginalSslContextProviders();

        ProviderList list = Providers.getProviderList();
        for (Provider provider : list.providers()) {

            if (SNIFFY_PROVIDER_NAME.equals(provider.getName())) continue;

            //System.out.println(provider.getName() + " - " + provider.getInfo());
            //System.out.println("=============");
            for (Provider.Service service : provider.getServices()) {
                //System.out.println(service.getType() + " - " + service.getAlgorithm() + " = " + service.getClassName());
                if (SSLCONTEXT.equals(service.getType())) {
                    SniffySSLContextProviderService sniffyService = new SniffySSLContextProviderService(
                            provider, service.getType(), service.getAlgorithm(), SniffySSLContextProviderService.class.getName(), new ArrayList<String>(), new HashMap<String, String>(), service
                    );

                    Map<Object, Object> legacyServiceMap = ReflectionUtil.getField(Provider.class, provider, "legacyMap");
                    Map<Object, Object> legacyStringMap = ReflectionUtil.getField(Provider.class, provider, "legacyStrings");

                    {
                        Set<Object> keys = new LinkedHashSet<Object>();

                        for (Map.Entry<Object, Object> entry : legacyServiceMap.entrySet()) {
                            if (service.equals(entry.getValue())) {
                                keys.add(entry.getKey());
                            }
                        }

                        for (Object key : keys) {
                            legacyServiceMap.remove(key);
                        }
                    }

                    {
                        Set<Object> keys = new LinkedHashSet<Object>();

                        for (Map.Entry<Object, Object> entry : legacyStringMap.entrySet()) {
                            if (service.getClassName().equals(entry.getValue())) {
                                keys.add(entry.getKey());
                            }
                        }

                        for (Object key : keys) {
                            legacyStringMap.remove(key);
                        }
                    }

                    //ReflectionUtil.invokeMethod(Provider.class, provider, "removePropertyStrings", Provider.Service.class, service, Void.class);
                    //ReflectionUtil.invokeMethod(Provider.class, provider, "removeService", Provider.Service.class, service, Void.class);
                    ReflectionUtil.invokeMethod(Provider.class, provider, "putService", Provider.Service.class, sniffyService, Void.class);

                    ReflectionUtil.setField(Provider.Service.class, service, "registered", true);
                }
            }
        }
    }

}
