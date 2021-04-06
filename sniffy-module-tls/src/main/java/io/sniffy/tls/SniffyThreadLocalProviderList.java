package io.sniffy.tls;

import io.sniffy.Constants;
import io.sniffy.util.JVMUtil;
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

                    if (!JVMUtil.isIbmJ9()) {

                        SniffySSLContextSpiProvider firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi = tuple.getValue();

                        if (null != firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi) {
                            Provider.Service defaultSniffySSLContextSpiProviderService =
                                    firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi.getService(SSLCONTEXT, "Default");
                            if (null != defaultSniffySSLContextSpiProviderService) {
                                try {
                                    SSLContext.setDefault(
                                            new SniffySSLContext(
                                                    (SSLContextSpi) defaultSniffySSLContextSpiProviderService.newInstance(null),
                                                    firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi,
                                                    "Default"
                                            )
                                    );
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    /*
                                    We're catching exception here because otherwise it fails on ibm-java-x86_64-80

                                    java.lang.ExceptionInInitializerError
                                        at java.lang.J9VMInternals.ensureError(J9VMInternals.java:146)
                                        at java.lang.J9VMInternals.recordInitializationFailure(J9VMInternals.java:135)
                                        at javax.crypto.KeyAgreement.getInstance(Unknown Source)
                                        at org.bouncycastle.jcajce.util.DefaultJcaJceHelper.createKeyAgreement(Unknown Source)
                                        at org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCrypto.isSupportedNamedGroup(Unknown Source)
                                        at org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCrypto.hasNamedGroup(Unknown Source)
                                        at org.bouncycastle.jsse.provider.NamedGroupInfo.addNamedGroup(Unknown Source)
                                        at org.bouncycastle.jsse.provider.NamedGroupInfo.createIndex(Unknown Source)
                                        at org.bouncycastle.jsse.provider.NamedGroupInfo.createPerContext(Unknown Source)
                                        at org.bouncycastle.jsse.provider.ContextData.<init>(Unknown Source)
                                        at org.bouncycastle.jsse.provider.ProvSSLContextSpi.engineInit(Unknown Source)
                                        at org.bouncycastle.jsse.provider.DefaultSSLContextSpi.<init>(Unknown Source)
                                        at org.bouncycastle.jsse.provider.BouncyCastleJsseProvider$8.createInstance(Unknown Source)
                                        at org.bouncycastle.jsse.provider.BouncyCastleJsseProvider$BcJsseService.newInstance(Unknown Source)
                                        at io.sniffy.tls.SniffySSLContextSpiProviderService.newInstance(SniffySSLContextSpiProviderService.java:21)
                                        at io.sniffy.tls.SniffyThreadLocalProviderList.set(SniffyThreadLocalProviderList.java:122)
                                        at io.sniffy.tls.SniffyThreadLocalProviderList.set(SniffyThreadLocalProviderList.java:19)
                                        at sun.security.jca.Providers.changeThreadProviderList(Providers.java:210)
                                        at sun.security.jca.Providers.setProviderList(Providers.java:158)
                                        at java.security.Security.insertProviderAt(Security.java:376)
                                        at io.sniffy.tls.DecryptBouncyCastleGoogleTrafficTest.testGoogleTraffic(DecryptBouncyCastleGoogleTrafficTest.java:36)
                                        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                                        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:90)
                                        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:55)
                                        at java.lang.reflect.Method.invoke(Method.java:508)
                                        at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
                                        at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
                                        at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
                                        at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
                                        at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
                                        at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
                                        at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
                                        at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
                                        at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
                                        at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
                                        at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
                                        at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                                        at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                                        at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
                                        at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
                                        at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
                                        at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
                                        at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
                                        at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
                                        at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:220)
                                        at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:53)
                                    Caused by: java.lang.SecurityException: Cannot set up certs for trusted CAs
                                        at javax.crypto.b.<clinit>(Unknown Source)
                                        ... 44 more
                                    Caused by: java.lang.SecurityException: Jurisdiction policy files are not signed by trusted signers!
                                        at javax.crypto.b.a(Unknown Source)
                                        at javax.crypto.b.c(Unknown Source)
                                        at javax.crypto.b.access$600(Unknown Source)
                                        at javax.crypto.b$a.run(Unknown Source)
                                        at java.security.AccessController.doPrivileged(AccessController.java:738)
                                        ... 45 more


                                    Process finished with exit code 255

                                     */
                                }
                            }
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
