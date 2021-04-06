package io.sniffy.tls;

import io.sniffy.Constants;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

public class SniffySecurityUtil {

    public static final String SSLCONTEXT = "SSLContext";

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

                        SniffySSLContextSpiProvider sniffySSLContextSpiProvider = new SniffySSLContextSpiProvider(originalSecurityProvider);

                        if (hasDefaultSSLContextService && null == firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi) {
                            firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi = sniffySSLContextSpiProvider;
                        }

                        Security.removeProvider(originalProviderName);
                        Security.insertProviderAt(sniffySSLContextSpiProvider, i + j + 1);

                        Security.insertProviderAt(
                                new SniffySSLContextSpiProvider(
                                        originalSecurityProvider,
                                        "Sniffy-" + originalProviderName,
                                        Constants.MAJOR_VERSION,
                                        "SniffySSLContextProvider"
                                ),
                                i + j + 1
                        );

                        j++;

                    }

                }

            }

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

        }

    }

    public static void uninstall() {

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

        }

    }

}
