package io.sniffy.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.1.7
 */
public class JVMUtil {

    /**
     * @return true if code is running inside sniffy tests; return false when used in other projects
     */
    public static boolean isTestingSniffy() {
        return null != System.getProperty("io.sniffy.test");
    }

    protected static Map<String, Object> MAP_FOR_GC = new HashMap<String,Object>();

    public static int invokeGarbageCollector() {

        int attempts = 0;

        try {
            Runtime runtime = Runtime.getRuntime();

            long totalMemory;

            outer:
            for (int j = 1; j < 10; j++) {
                for (int i = 0; i < 1000; i++) {

                    attempts++;

                    totalMemory = runtime.totalMemory();

                    System.gc();
                    System.gc();

                    if (runtime.totalMemory() != totalMemory) {
                        break outer;
                    } else {
                        MAP_FOR_GC.put("io.sniffy.testing.dummy." + i, new byte[1024 * 1024]);
                        if (i > j) {
                            MAP_FOR_GC.remove("io.sniffy.testing.dummy." + (i - j));
                        }
                    }
                }
            }
        } catch (OutOfMemoryError oom) {
            MAP_FOR_GC = new HashMap<String, Object>();
            System.gc();
            System.gc();
        }

        return attempts;

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
