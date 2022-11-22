package io.sniffy.tls;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.ReflectionUtil;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;

import java.lang.reflect.InvocationTargetException;

public class SniffyProviderListUtil {

    private static final Polyglog LOG = PolyglogFactory.log(SniffyProviderListUtil.class);

    public static void install() {

        LOG.info("Setting Providers.threadLists to SniffyThreadLocalProviderList");
        SniffyThreadLocalProviderList sniffyThreadLocalProviderList = new SniffyThreadLocalProviderList();
        ReflectionUtil.setField(Providers.class, null, "threadLists", sniffyThreadLocalProviderList);

        LOG.info("Setting Providers.threadListsUsed to 1");
        ReflectionUtil.setField(Providers.class, null, "threadListsUsed", 1);

        // now let us verify that Sniffy JSSE provider interceptor was installed correctly

        Providers.beginThreadProviderList(ProviderList.newList());
        ProviderList threadProviderList = Providers.getThreadProviderList();
        LOG.trace("Providers.getThreadProviderList() = " + threadProviderList);
        if (null == threadProviderList) {
            LOG.error("SniffyThreadLocalProviderList doesn't work - probably because Providers.threadLists variable was inlined by JVM. Try loading Sniffy at earlier stage - see https://sniffy.io/docs/ for details");
        }
        Providers.endThreadProviderList(null);

    }

    public static void uninstall() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        LOG.info("Setting Providers.threadListsUsed to 0");
        ReflectionUtil.setField(Providers.class, null, "threadListsUsed", 0);

        LOG.info("Setting Providers.threadLists to new ThreadLocal<ProviderList>()");
        ReflectionUtil.setField(Providers.class, null, "threadLists", new ThreadLocal<ProviderList>());

    }

}
