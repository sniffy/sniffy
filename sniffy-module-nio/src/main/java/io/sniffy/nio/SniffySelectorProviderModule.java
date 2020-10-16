package io.sniffy.nio;

public class SniffySelectorProviderModule {

    private static int getVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        if (version.contains("-")) {
            version = version.substring(version.indexOf("-"));
        }
        return Integer.parseInt(version);
    }

    public static void initialize() {

        if (getVersion() <= 8) return; // TODO: change to 7

        try {
            SniffySelectorProvider.install();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
