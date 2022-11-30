package io.sniffy.tls;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.field.StaticFieldRef;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;

import static io.sniffy.reflection.Unsafe.$;

public class SniffyProviderListUtil {

    private static final Polyglog LOG = PolyglogFactory.log(SniffyProviderListUtil.class);

    public static void install() {

        // TODO: add reflection based tests here to check if ThreadLocal and other fields are in place
        /*LOG.info("Setting Providers.threadLists to SniffyThreadLocalProviderList");
        SniffyThreadLocalProviderList sniffyThreadLocalProviderList = new SniffyThreadLocalProviderList();
        try {
            $(Providers.class).<ThreadLocal<ProviderList>>getStaticField("threadLists").set(sniffyThreadLocalProviderList);
        } catch (Exception e) {
            LOG.error(e);
        }

        LOG.info("Setting Providers.threadListsUsed to 1");
        try {
            // TODO: only do on success from step above
            $(Providers.class).<Integer>getStaticField("threadListsUsed").set(1);
        } catch (Exception e) {
            LOG.error(e);
        }*/

        try {
            //assert null == Providers.beginThreadProviderList(ProviderList.newList()); // trick JDK

            ProvidersWrapper providersWrapper = new ProvidersWrapper();
            $(Providers.class).<ThreadLocal<ProviderList>>getStaticField("threadLists").set(providersWrapper);
        } catch (Exception e) {
            LOG.error(e);
        }

        try {
            StaticFieldRef<Integer> threadListsUsed = $(Providers.class).<Integer>getStaticField("threadListsUsed").resolve();
            synchronized (Providers.class) {
                threadListsUsed.set(1);
                //threadListsUsed.set(threadListsUsed.get() + 1);
            }
        } catch (Exception e) {
            LOG.error(e);
        }

        // now let us verify that Sniffy JSSE provider interceptor was installed correctly


        try {
            LOG.trace("Providers.threadListsUsed = " + $(Providers.class).getStaticField("threadListsUsed").get());
            LOG.trace("Providers.threadLists = " + $(Providers.class).getStaticField("threadLists").get());
            LOG.trace("Providers.threadLists.get() = " + $(Providers.class).<ThreadLocal<ProviderList>>getStaticField("threadLists").get().get());
            LOG.trace("Providers.providerList = " + $(Providers.class).getStaticField("providerList").get());
        } catch (Exception e) {
            assert false : e;
        }

        LOG.trace("Providers.beginThreadProviderList(ProviderList.newList());");
        Providers.beginThreadProviderList(ProviderList.newList());

        try {
            LOG.trace("Providers.threadListsUsed = " + $(Providers.class).getStaticField("threadListsUsed").get());
            LOG.trace("Providers.threadLists = " + $(Providers.class).getStaticField("threadLists").get());
            LOG.trace("Providers.threadLists.get() = " + $(Providers.class).<ThreadLocal<ProviderList>>getStaticField("threadLists").get().get());
            LOG.trace("Providers.providerList = " + $(Providers.class).getStaticField("providerList").get());
        } catch (Exception e) {
            assert false : e;
        }

        ProviderList threadProviderList = Providers.getThreadProviderList();
        LOG.trace("Providers.getThreadProviderList() = " + threadProviderList);
        if (null == threadProviderList) {
            ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
            assert null != threadProviderList;
            LOG.error("SniffyThreadLocalProviderList doesn't work - probably because Providers.threadLists variable was inlined by JVM. Try loading Sniffy at earlier stage - see https://sniffy.io/docs/ for details");
        }
        Providers.endThreadProviderList(null);

    }

    public static void uninstall() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        LOG.info("Setting Providers.threadListsUsed to 0");
        try {
            StaticFieldRef<Integer> threadListsUsed = $(Providers.class).<Integer>getStaticField("threadListsUsed").resolve();
            synchronized (Providers.class) {
                // threadListsUsed.set(threadListsUsed.get() - 1); // TODO: it breaks SniffySSLContextSpiProviderTest.testUninstall()
                threadListsUsed.set(0);
            }
        } catch (Exception e) {
            LOG.error(e);
        }

        LOG.info("Setting Providers.threadLists to new ThreadLocal<ProviderList>()");
        try {
            StaticFieldRef<ThreadLocal<ProviderList>> threadLists = $(Providers.class).<ThreadLocal<ProviderList>>getStaticField("threadLists").resolve();
            threadLists.set(new ThreadLocal<ProviderList>());
        } catch (Exception e) {
            LOG.error(e);
        }

    }

}
