package io.sniffy.tls;

import io.sniffy.util.ReflectionUtil;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;

import java.lang.reflect.InvocationTargetException;

public class SniffyProviderListUtil {

    public static void install() {

        ReflectionUtil.setField(Providers.class, null, "threadListsUsed", 1);
        ReflectionUtil.setField(Providers.class, null, "threadLists", new SniffyThreadLocalProviderList());

    }

    public static void uninstall() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        ReflectionUtil.setField(Providers.class, null, "threadListsUsed", 0);
        ReflectionUtil.setField(Providers.class, null, "threadLists", new ThreadLocal<ProviderList>());

    }

}
