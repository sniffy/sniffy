package io.sniffy.util;

/**
 * @since 3.1.7
 */
public class OSUtil {

    private static String osName = System.getProperty("os.name");

    private OSUtil() {
    }

    public static boolean isWindows()
    {
        return null != osName && osName.startsWith("Windows");
    }

}
