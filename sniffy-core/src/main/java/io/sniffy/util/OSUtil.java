package io.sniffy.util;

import java.util.Locale;

/**
 * @since 3.1.7
 */
public class OSUtil {

    private static String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    private OSUtil() {
    }

    public static boolean isWindows()
    {
        return null != osName && osName.contains("windows");
    }

    public static boolean isMac()
    {
        return null != osName && (osName.contains("mac") || osName.contains("darwin"));
    }

    public static String getOsName() {
        return osName;
    }

}
