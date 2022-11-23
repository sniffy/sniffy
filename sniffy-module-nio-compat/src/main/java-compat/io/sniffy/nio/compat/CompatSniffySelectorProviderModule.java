package io.sniffy.nio.compat;

import io.sniffy.reflection.Unsafe;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1.7
 */
@SuppressWarnings("unused")
public class CompatSniffySelectorProviderModule {

    public static void initialize() {

        if (Unsafe.getJavaVersion() >= 9) return;

        if (Unsafe.getJavaVersion() == 8 && !Boolean.getBoolean("io.sniffy.forceJava7Compatibility")) return;


        try {
            $("io.sniffy.nio.compat.CompatSniffySelectorProviderBootstrap").getStaticMethod("initialize").invoke();
            $("io.sniffy.nio.compat.CompatSniffySelectorProvider").getStaticMethod("install").invoke();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
