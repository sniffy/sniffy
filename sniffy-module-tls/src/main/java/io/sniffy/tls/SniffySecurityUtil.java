package io.sniffy.tls;

import io.sniffy.Constants;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.ReflectionUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLSocketFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

public class SniffySecurityUtil {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySecurityUtil.class);

    public static final String SSLCONTEXT = "SSLContext";

    public static void wrapJsseProvidersWithSniffy() throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException, NoSuchAlgorithmException {

        synchronized (Security.class) {

            Provider[] originalSecurityProviders = Security.getProviders();

            LOG.info("Original security providers are " + Arrays.toString(originalSecurityProviders));

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

                        LOG.info("Original provider " + originalProviderName + " provides SSLContextSPI service - wrapped with " + sniffySSLContextSpiProvider);

                        if (hasDefaultSSLContextService && null == firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi) {
                            firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi = sniffySSLContextSpiProvider;
                        }

                        Security.removeProvider(originalProviderName);

                        // TODO: why do we add it twice? is it because of name? document it!
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
                    LOG.info("Identified default SSLContext service provider - " + firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi + " with service " + defaultSniffySSLContextSpiProviderService);
                    SniffySSLContext defaultSniffySSLContext = new SniffySSLContext(
                            (SSLContextSpi) defaultSniffySSLContextSpiProviderService.newInstance(null),
                            firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi,
                            "Default"
                    );
                    LOG.info("Setting SSLContext.default to " + defaultSniffySSLContext);
                    SSLContext.setDefault(defaultSniffySSLContext);
                    if (JVMUtil.getVersion() >= 13) {
                        LOG.info("Java 13+ detected - attempt to update javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder");

                        SSLSocketFactory originalSSLSocketFactory = ReflectionUtil.getFirstField("javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder", null, SSLSocketFactory.class);
                        if (null != originalSSLSocketFactory) {
                            SniffySSLSocketFactory sniffySSLSocketFactory = new SniffySSLSocketFactory(originalSSLSocketFactory);
                            LOG.info("Replacing " + originalSSLSocketFactory + " with " + sniffySSLSocketFactory);
                            ReflectionUtil.setFields(
                                    "javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder",
                                    null,
                                    SSLSocketFactory.class,
                                    sniffySSLSocketFactory);
                        }
                    } else {
                        LOG.info("Java 12- detected - attempt to update singleton inside javax.net.ssl.SSLSocketFactory");

                        SSLSocketFactory originalSSLSocketFactory = ReflectionUtil.getFirstField(SSLSocketFactory.class, null, SSLSocketFactory.class);
                        if (null != originalSSLSocketFactory) {
                            SniffySSLSocketFactory sniffySSLSocketFactory = new SniffySSLSocketFactory(originalSSLSocketFactory);
                            LOG.info("Replacing " + originalSSLSocketFactory + " with " + sniffySSLSocketFactory);
                            ReflectionUtil.setFields(
                                    SSLSocketFactory.class,
                                    null,
                                    SSLSocketFactory.class,
                                    sniffySSLSocketFactory);
                        }
                    }
                }
            }

        }

    }

    public static void uninstall() throws NoSuchAlgorithmException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException {

        LOG.info("Uninstalling Sniffy JSSE providers and wrappers");

        synchronized (Security.class) {

            Provider[] providers = Security.getProviders();

            LOG.info("Wrapped security providers are " + Arrays.toString(providers));

            for (int i = 0, j = 1; i < providers.length; i++) {

                Provider provider = providers[i];

                if (provider instanceof SniffySSLContextSpiProvider) {

                    if (provider.getName().startsWith("Sniffy-")) {
                        Security.removeProvider(provider.getName());
                    } else {
                        Security.removeProvider(provider.getName());
                        Provider originalProvider = ((SniffySSLContextSpiProvider) provider).getOriginalProvider();
                        Security.insertProviderAt(originalProvider, j);
                        LOG.info("Unwrapped provider " + provider.getName() + "; replaced " + provider + " with " + originalProvider);
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
                    LOG.info("Identified default SSLContext service provider - " + firstSSLContextSpiProviderWithDefaultSSLContextSpi + " with service " + defaultSSLContextSpiProviderService);
                    // TODO: don't rollback to SniffySSLContext even though it doesn do anything Sniffy
                    SniffySSLContext defaultSSLContext = new SniffySSLContext(
                            (SSLContextSpi) defaultSSLContextSpiProviderService.newInstance(null),
                            firstSSLContextSpiProviderWithDefaultSSLContextSpi,
                            "Default"
                    );
                    LOG.info("Setting SSLContext.default to " + defaultSSLContext);
                    SSLContext.setDefault(defaultSSLContext);
                    if (JVMUtil.getVersion() >= 13) {
                        LOG.info("Java 13+ detected - attempt to update javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder");

                        SSLSocketFactory sniffySSLSocketFactory = ReflectionUtil.getFirstField("javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder", null, SSLSocketFactory.class);
                        if (sniffySSLSocketFactory instanceof SniffySSLSocketFactory) {
                            SSLSocketFactory originalSSLSocketFactory = ((SniffySSLSocketFactory) sniffySSLSocketFactory).getDelegate();
                            LOG.info("Replacing " + sniffySSLSocketFactory + " with " + originalSSLSocketFactory);
                            ReflectionUtil.setFields(
                                    "javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder",
                                    null,
                                    SSLSocketFactory.class,
                                    originalSSLSocketFactory
                            );
                        } else if (null == sniffySSLSocketFactory) {
                            LOG.info("Removing javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder");
                            ReflectionUtil.setFields(
                                    "javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder",
                                    null,
                                    SSLSocketFactory.class,
                                    null
                            );
                        }
                    } else {
                        LOG.info("Java 12- detected - attempt to update singleton inside javax.net.ssl.SSLSocketFactory");

                        SSLSocketFactory sniffySSLSocketFactory = ReflectionUtil.getFirstField(SSLSocketFactory.class, null, SSLSocketFactory.class);
                        if (sniffySSLSocketFactory instanceof SniffySSLSocketFactory) {
                            SSLSocketFactory originalSSLSocketFactory = ((SniffySSLSocketFactory) sniffySSLSocketFactory).getDelegate();
                            LOG.info("Replacing " + sniffySSLSocketFactory + " with " + originalSSLSocketFactory);
                            ReflectionUtil.setFields(
                                    SSLSocketFactory.class,
                                    null,
                                    SSLSocketFactory.class,
                                    originalSSLSocketFactory
                            );
                        } else if (null == sniffySSLSocketFactory) {
                            LOG.info("Removing javax.net.ssl.SSLSocketFactory static fields of type SSLSocketFactory");
                            ReflectionUtil.setFields(SSLSocketFactory.class, null, SSLSocketFactory.class, null);
                            LOG.info("Setting javax.net.ssl.SSLSocketFactory static fields of type boolean to false");
                            ReflectionUtil.setFirstField(SSLSocketFactory.class, null, Boolean.TYPE, false);
                        }
                    }
                }
            }

        }

    }

}
