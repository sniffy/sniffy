package io.sniffy.nio;

import io.sniffy.util.JVMUtil;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1.7
 */
public class SniffySelectorProviderModule {

    public static void initialize() {

        if (JVMUtil.getVersion() <= 7) return;

        if (JVMUtil.getVersion() == 8 && Boolean.getBoolean("io.sniffy.forceJava7Compatibility")) return;

        if (JVMUtil.getVersion() >= 16) {

            try {
                $("sun.nio.ch.SelChImpl").moduleRef().addOpens("sun.nio.ch");
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
