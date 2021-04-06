package io.sniffy.util;

/**
 * @since 3.1.7
 */
public class JVMUtil {

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

    public static boolean isIbmJ9() {
        return "IBM J9 VM".equals(System.getProperty("java.vm.name"));
    }

}
