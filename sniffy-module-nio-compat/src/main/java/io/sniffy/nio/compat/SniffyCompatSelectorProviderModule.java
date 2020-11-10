package io.sniffy.nio.compat;

import io.sniffy.util.JVMUtil;

/**
 * @since 3.1.7
 */
public class SniffyCompatSelectorProviderModule {

    public static void initialize() {

        if (JVMUtil.getVersion() >= 8) return; // TODO: change to 8

        try {
            Class.forName("io.sniffy.nio.compat.CompatSniffySelectorProviderBootstrap").getMethod("initialize").invoke(null);
            Class.forName("io.sniffy.nio.compat.CompatSniffySelectorProvider").getMethod("install").invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
