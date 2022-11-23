package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.Unsafe;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1.7
 */
public class SniffySelectorProviderModule {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelectorProviderModule.class);

    public static void initialize() {

        if (Unsafe.getJavaVersion() <= 7) return;

        if (Unsafe.getJavaVersion() == 8 && Boolean.getBoolean("io.sniffy.forceJava7Compatibility")) return;

        if (Unsafe.getJavaVersion() >= 16) {
            if (!$("sun.nio.ch.SelChImpl").tryGetModuleRef().tryAddOpens("sun.nio.ch")) {
                LOG.error("Couldn't open module with sun.nio.ch.SelChImpl class");
            }
        }

        try {
            SniffySelectorProvider.install();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
