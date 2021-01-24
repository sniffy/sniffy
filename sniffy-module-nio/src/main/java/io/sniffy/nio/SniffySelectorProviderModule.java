package io.sniffy.nio;

import io.sniffy.util.JVMUtil;
import io.sniffy.util.ReflectionUtil;

import java.lang.reflect.Method;

/**
 * @since 3.1.7
 */
public class SniffySelectorProviderModule {

    public static void initialize() {

        if (JVMUtil.getVersion() <= 7) return;

        if (JVMUtil.getVersion() == 8 && Boolean.getBoolean("io.sniffy.forceJava7Compatibility")) return;

        if (JVMUtil.getVersion() >= 16) {

            try {
                Class<?> moduleClass = Class.forName("java.lang.Module");
                Method implAddOpensMethod = moduleClass.getDeclaredMethod("implAddOpens", String.class);
                ReflectionUtil.setAccessible(implAddOpensMethod);

                Class<?> selChImplClass = Class.forName("sun.nio.ch.SelChImpl");
                Method getModuleMethod = Class.class.getMethod("getModule");

                Object module = getModuleMethod.invoke(selChImplClass);
                implAddOpensMethod.invoke(module, "sun.nio.ch");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            SniffySelectorProvider.install();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
