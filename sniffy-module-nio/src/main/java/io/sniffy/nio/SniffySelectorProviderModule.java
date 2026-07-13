package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;

/**
 * @since 3.1.7
 */
public class SniffySelectorProviderModule {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelectorProviderModule.class);
    private static boolean unsupportedRuntimeLogged;

    public static boolean initialize() {
        try {
            // Resolve module access before the provider class links channel wrappers implementing SelChImpl.
            JdkNioAccess.resolve();
            return SniffySelectorProvider.installWithResult().isInstalled();
        } catch (JdkNioAccess.JdkNioAccessException e) {
            synchronized (SniffySelectorProviderModule.class) {
                if (!unsupportedRuntimeLogged) {
                    LOG.error("NIO monitoring is unsupported on this runtime; classic socket monitoring remains active", e);
                    unsupportedRuntimeLogged = true;
                }
            }
            return false;
        }
    }

}
