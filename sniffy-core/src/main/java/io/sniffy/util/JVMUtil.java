package io.sniffy.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 3.1.7
 */
public class JVMUtil {

    private final static Map<String, Boolean> CLASS_CACHE = new ConcurrentHashMap<String, Boolean>();

    /**
     * @return true if code is running inside sniffy tests; return false when used in other projects
     */
    public static boolean isTestingSniffy() {
        return null != System.getProperty("io.sniffy.test");
    }

    public static boolean hasJUnitOnClassPath() {
        return hasClassAvailable("org.junit.Test");
    }

    public static boolean hasClassAvailable(String className) {
        Boolean available = CLASS_CACHE.get(className);
        if (null != available) {
            return available;
        } else {
            synchronized (CLASS_CACHE) {
                available = true;
                try {
                    Class.forName(className);
                } catch (ClassNotFoundException e) {
                    available = false;
                }
                CLASS_CACHE.put(className, available);
            }
        }
        return available;
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
