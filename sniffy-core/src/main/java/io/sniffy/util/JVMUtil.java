package io.sniffy.util;

import java.util.Locale;

/**
 * @since 3.1.7
 */
public class JVMUtil {

    public static boolean isJ9() {
        String vmName = System.getProperty("java.vm.name", "");
        String normalized = vmName.toLowerCase(Locale.ROOT);

        return normalized.contains("openj9")
                || normalized.contains("j9");
    }

    public static int getVersion() {
        String version = System.getProperty("java.version");
        if (null == version) {
            return 8; // TODO: log it
        }
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        if (version.contains("-")) {
            version = version.substring(0, version.indexOf("-"));
        }
        return Integer.parseInt(version);
    }

}
