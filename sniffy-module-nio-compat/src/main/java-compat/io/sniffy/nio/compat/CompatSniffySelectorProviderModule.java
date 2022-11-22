package io.sniffy.nio.compat;

import io.sniffy.util.JVMUtil;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1.7
 */
public class CompatSniffySelectorProviderModule {

    public static void initialize() {

        if (JVMUtil.getVersion() >= 9) return;

        if (JVMUtil.getVersion() == 8 && !Boolean.getBoolean("io.sniffy.forceJava7Compatibility")) return;


        try {
            $("io.sniffy.nio.compat.CompatSniffySelectorProviderBootstrap").method("initialize").invoke(null);
            $("io.sniffy.nio.compat.CompatSniffySelectorProvider").method("install").invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
