package io.sniffy.tls;

import io.sniffy.Constants;
import io.sniffy.util.ReflectionUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLSocketFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

public class SniffySecurityUtil {

    public static final String SSLCONTEXT = "SSLContext";

    private static SSLSocketFactory originalSSLSocketFactory;

    public static void wrapJsseProvidersWithSniffy() throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException, NoSuchAlgorithmException {

        synchronized (Security.class) {

            Provider[] originalSecurityProviders = Security.getProviders();

            SniffySSLContextSpiProvider firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi = null;

            for (int i = 0, j = 0; i < originalSecurityProviders.length; i++) {

                Provider originalSecurityProvider = originalSecurityProviders[i];

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

                        SniffySSLContextSpiProvider sniffySSLContextSpiProvider = new SniffySSLContextSpiProvider(
                                originalSecurityProvider,
                                "Sniffy-" + originalProviderName,
                                Constants.MAJOR_VERSION,
                                "SniffySSLContextProvider"
                        );

                        if (hasDefaultSSLContextService && null == firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi) {
                            firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi = sniffySSLContextSpiProvider;
                        }

                        Security.removeProvider(originalProviderName);

                        Security.insertProviderAt(new SniffySSLContextSpiProvider(originalSecurityProvider), i + j + 1);
                        Security.insertProviderAt(sniffySSLContextSpiProvider, i + j + 1);

                        j++;

                    }

                }

            }

            if (null != firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi) {
                Provider.Service defaultSniffySSLContextSpiProviderService =
                        firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi.getService(SSLCONTEXT, "Default");
                if (null != defaultSniffySSLContextSpiProviderService) {
                    SniffySSLContext defaultSniffySSLContext = new SniffySSLContext(
                            (SSLContextSpi) defaultSniffySSLContextSpiProviderService.newInstance(null),
                            firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi,
                            "Default"
                    );
                    SSLContext.setDefault(defaultSniffySSLContext);
                    originalSSLSocketFactory = ReflectionUtil.getField(SSLSocketFactory.class, null, "b");
                    ReflectionUtil.setField(SSLSocketFactory.class, null, "b", defaultSniffySSLContext.getSocketFactory()); // TODO: instead we should wrap delegate
                }
            }

        }

    }

    public static void uninstall() throws NoSuchAlgorithmException {

        synchronized (Security.class) {

            Provider[] providers = Security.getProviders();

            for (int i = 0, j = 1; i < providers.length; i++) {

                Provider provider = providers[i];

                if (provider instanceof SniffySSLContextSpiProvider) {

                    if (provider.getName().startsWith("Sniffy-")) {
                        Security.removeProvider(provider.getName());
                    } else {
                        Security.removeProvider(provider.getName());
                        Provider originalProvider = ((SniffySSLContextSpiProvider) provider).getOriginalProvider();
                        Security.insertProviderAt(originalProvider, j);
                        j++;
                    }

                } else {
                    j++;
                }

            }

            Provider firstSSLContextSpiProviderWithDefaultSSLContextSpi = null;

            for (Provider provider : Security.getProviders()) {
                for (Provider.Service service : provider.getServices()) {
                    if (SSLCONTEXT.equals(service.getType())) {
                        if ("Default".equalsIgnoreCase(service.getAlgorithm())) {
                            firstSSLContextSpiProviderWithDefaultSSLContextSpi = provider;
                            break;
                        }
                    }
                }
            }

            if (null != firstSSLContextSpiProviderWithDefaultSSLContextSpi) {
                Provider.Service defaultSSLContextSpiProviderService =
                        firstSSLContextSpiProviderWithDefaultSSLContextSpi.getService(SSLCONTEXT, "Default");
                if (null != defaultSSLContextSpiProviderService) {
                    SniffySSLContext defaultSSLContext = new SniffySSLContext(
                            (SSLContextSpi) defaultSSLContextSpiProviderService.newInstance(null),
                            firstSSLContextSpiProviderWithDefaultSSLContextSpi,
                            "Default"
                    );
                    SSLContext.setDefault(defaultSSLContext);
                    ReflectionUtil.setField(SSLSocketFactory.class, null, "b", originalSSLSocketFactory);
                }
            }

        }

    }

}
