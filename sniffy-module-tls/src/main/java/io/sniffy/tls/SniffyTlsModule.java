package io.sniffy.tls;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.JVMUtil;

import static io.sniffy.reflection.Unsafe.$;

public class SniffyTlsModule {

    private static final Polyglog LOG = PolyglogFactory.log(SniffyTlsModule.class);

    public static void initialize() {

        if (JVMUtil.getVersion() >= 16) {
            if (!$("sun.security.jca.Providers").tryGetModuleRef().tryAddOpens("sun.security.jca")) {
                LOG.error("Couldn't open module with sun.security.jca.Providers class");
            }
        }

        try {
            LOG.info("Installing interceptor for installing new JSSE providers");
            SniffyProviderListUtil.install();
            LOG.info("Installed interceptor for installing new JSSE providers");
        } catch (Exception e) {
            LOG.error(e);
        }

        try {
            LOG.info("Installing Sniffy JSSE provider");
            SniffySecurityUtil.wrapJsseProvidersWithSniffy();
            LOG.info("Installed Sniffy JSSE provider");
        } catch (Exception e) {
            LOG.error(e);
        }

    }

    public static void reinitialize() {

        try {
            LOG.info("Installing Sniffy JSSE provider");
            SniffySecurityUtil.wrapJsseProvidersWithSniffy();
            LOG.info("Installed Sniffy JSSE provider");
        } catch (Exception e) {
            LOG.error(e);
        }

    }

    public static void uninstall() {

        try {
            LOG.info("Uninstalling Sniffy JSSE provider");
            SniffyProviderListUtil.uninstall();
            LOG.info("Uninstalled Sniffy JSSE provider");
        } catch (Exception e) {
            LOG.error(e);
        }

        try {
            LOG.info("Uninstalling interceptor for installing new JSSE providers");
            SniffySecurityUtil.uninstall();
            LOG.info("Uninstalled interceptor for installing new JSSE providers");
        } catch (Exception e) {
            LOG.error(e);
        }

    }

}
