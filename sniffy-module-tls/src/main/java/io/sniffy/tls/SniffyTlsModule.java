package io.sniffy.tls;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.ReflectionUtil;

import java.lang.reflect.Method;

public class SniffyTlsModule {

    private static final Polyglog LOG = PolyglogFactory.log(SniffyTlsModule.class);

    public static void initialize() {

        if (JVMUtil.getVersion() >= 16) {

            LOG.debug("Java 16+ detected - opening module sun.security.jca");

            try {
                Class<?> moduleClass = Class.forName("java.lang.Module");
                Method implAddOpensMethod = moduleClass.getDeclaredMethod("implAddOpens", String.class);
                ReflectionUtil.setAccessible(implAddOpensMethod);

                Class<?> selChImplClass = Class.forName("sun.security.jca.Providers");
                //noinspection JavaReflectionMemberAccess
                Method getModuleMethod = Class.class.getMethod("getModule");

                Object module = getModuleMethod.invoke(selChImplClass);
                implAddOpensMethod.invoke(module, "sun.security.jca");

            } catch (Exception e) {
                LOG.error(e);
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
