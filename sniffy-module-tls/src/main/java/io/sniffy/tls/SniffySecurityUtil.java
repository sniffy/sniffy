package io.sniffy.tls;

import io.sniffy.Constants;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.Unsafe;
import io.sniffy.reflection.UnsafeInvocationException;
import io.sniffy.reflection.field.FieldFilters;
import io.sniffy.reflection.field.UnresolvedStaticFieldRef;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLSocketFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

import static io.sniffy.reflection.Unsafe.$;

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
                        try {
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
                        } catch (UnresolvedRefException e) {
                            throw Unsafe.throwException(e);
                        } catch (UnsafeInvocationException e) {
                            throw Unsafe.throwException(e);
                        }

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
                    if (Unsafe.getJavaVersion() >= 13) {
                        try {
                            LOG.info("Java 13+ detected - attempt to update javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder");
                            UnresolvedStaticFieldRef<SSLSocketFactory> sslSocketFactoryFieldRef =
                                    $("javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder").tryFindFirstStaticField(FieldFilters.ofType(SSLSocketFactory.class), true);
                            if (sslSocketFactoryFieldRef.isResolved()) {
                                SSLSocketFactory originalSSLSocketFactory = sslSocketFactoryFieldRef.get();
                                if (null != originalSSLSocketFactory) {
                                    SniffySSLSocketFactory sniffySSLSocketFactory = new SniffySSLSocketFactory(originalSSLSocketFactory);
                                    LOG.info("Replacing " + originalSSLSocketFactory + " with " + sniffySSLSocketFactory);
                                    sslSocketFactoryFieldRef.set(sniffySSLSocketFactory);
                                } else {
                                    LOG.error("Original SSLSocketFactory was null");
                                }
                            }
                        } catch (UnresolvedRefException e) {
                            throw Unsafe.throwException(e);
                        } catch (UnsafeInvocationException e) {
                            throw Unsafe.throwException(e);
                        }
                    } else {
                        LOG.info("Java 12- detected - attempt to update singleton inside javax.net.ssl.SSLSocketFactory");
                        try {
                            LOG.info("Java 12- detected - attempt to update javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder");
                            UnresolvedStaticFieldRef<SSLSocketFactory> sslSocketFactoryFieldRef =
                                    $(SSLSocketFactory.class).findFirstStaticField(FieldFilters.ofType(SSLSocketFactory.class), true);
                            if (sslSocketFactoryFieldRef.isResolved()) {
                                SSLSocketFactory originalSSLSocketFactory = sslSocketFactoryFieldRef.get();
                                if (null != originalSSLSocketFactory) {
                                    SniffySSLSocketFactory sniffySSLSocketFactory = new SniffySSLSocketFactory(originalSSLSocketFactory);
                                    LOG.info("Replacing " + originalSSLSocketFactory + " with " + sniffySSLSocketFactory);
                                    sslSocketFactoryFieldRef.set(sniffySSLSocketFactory);
                                } else {
                                    LOG.error("Original SSLSocketFactory was null");
                                }
                            }
                        } catch (UnresolvedRefException e) {
                            throw Unsafe.throwException(e);
                        } catch (UnsafeInvocationException e) {
                            throw Unsafe.throwException(e);
                        }
                        // TODO: warn if couldn't find all sslsocket factories
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
                    if (Unsafe.getJavaVersion() >= 13) {
                        LOG.info("Java 13+ detected - attempt to update javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder");
                        try {
                            LOG.info("Java 13+ detected - attempt to update javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder");
                            UnresolvedStaticFieldRef<SSLSocketFactory> sslSocketFactoryFieldRef =
                                    $("javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder").tryFindFirstStaticField(FieldFilters.ofType(SSLSocketFactory.class), true);
                            if (sslSocketFactoryFieldRef.isResolved()) {
                                SSLSocketFactory sniffySSLSocketFactory = sslSocketFactoryFieldRef.get();
                                if (sniffySSLSocketFactory instanceof SniffySSLSocketFactory) {
                                    SSLSocketFactory originalSSLSocketFactory = ((SniffySSLSocketFactory) sniffySSLSocketFactory).getDelegate();
                                    LOG.info("Replacing " + sniffySSLSocketFactory + " with " + originalSSLSocketFactory);
                                    sslSocketFactoryFieldRef.set(originalSSLSocketFactory);
                                } else {
                                    LOG.error("Original SSLSocketFactory was null");
                                    sslSocketFactoryFieldRef.set(null);
                                }
                            }
                        } catch (UnresolvedRefException e) {
                            throw Unsafe.throwException(e);
                        } catch (UnsafeInvocationException e) {
                            throw Unsafe.throwException(e);
                        }
                    } else {
                        LOG.info("Java 12- detected - attempt to update singleton inside javax.net.ssl.SSLSocketFactory");
                        try {
                            UnresolvedStaticFieldRef<SSLSocketFactory> sslSocketFactoryFieldRef =
                                    $(SSLSocketFactory.class).findFirstStaticField(FieldFilters.ofType(SSLSocketFactory.class), true);
                            if (sslSocketFactoryFieldRef.isResolved()) {
                                SSLSocketFactory sniffySSLSocketFactory = sslSocketFactoryFieldRef.get();
                                if (sniffySSLSocketFactory instanceof SniffySSLSocketFactory) {
                                    SSLSocketFactory originalSSLSocketFactory = ((SniffySSLSocketFactory) sniffySSLSocketFactory).getDelegate();
                                    LOG.info("Replacing " + sniffySSLSocketFactory + " with " + originalSSLSocketFactory);
                                    sslSocketFactoryFieldRef.set(originalSSLSocketFactory);
                                } else {
                                    LOG.info("Removing javax.net.ssl.SSLSocketFactory static fields of type SSLSocketFactory");
                                    sslSocketFactoryFieldRef.set(null);
                                    UnresolvedStaticFieldRef<Object> propertyCheckedFieldRef = $(SSLSocketFactory.class).getStaticField("propertyChecked");
                                    if (propertyCheckedFieldRef.isResolved()) {
                                        propertyCheckedFieldRef.set(null);
                                    }
                                }
                            }
                        } catch (UnresolvedRefException e) {
                            throw Unsafe.throwException(e);
                        } catch (UnsafeInvocationException e) {
                            throw Unsafe.throwException(e);
                        }

                    }
                }
            }

        }

    }

}
