package io.sniffy.nio;

import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

public class SniffySelectorProviderBootstrap {

    private static boolean publicSelChImplLoadedInBootstrapClassLoader = false;

    private static int getVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }

    public static void loadPublicSelChImplInBootstrapClassLoader() throws Exception {

        if (publicSelChImplLoadedInBootstrapClassLoader) return;

        publicSelChImplLoadedInBootstrapClassLoader = true;

        if (getVersion() >= 9) return;

        InputStream is = SniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream("sun/nio/ch/PublicSelChImpl.clazz");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int i = 0;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }

        is.close();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        unsafe.defineClass(
                "sun.nio.ch.PublicSelChImpl",
                baos.toByteArray(),
                0,
                baos.size(),
                Class.forName("sun.nio.ch.SelChImpl").getClassLoader(),
                new ProtectionDomain(
                        new CodeSource(
                                SniffySelectorProviderBootstrap.class.getClassLoader().getResource("sun/nio/ch/PublicSelChImpl.clazz"),
                                new Certificate[] {}
                        ), new Permissions())
        );

    }

    public static void initialize() {

        try {
            loadPublicSelChImplInBootstrapClassLoader();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
