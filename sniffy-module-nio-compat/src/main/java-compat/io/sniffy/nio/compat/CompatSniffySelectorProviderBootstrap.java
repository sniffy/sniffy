package io.sniffy.nio.compat;

import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * @since 3.1.7
 */
public class CompatSniffySelectorProviderBootstrap {

    private static boolean publicSelChImplLoadedInBootstrapClassLoader = false;

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
        return Integer.parseInt(version);
    }

    public static void loadPublicSelChImplInBootstrapClassLoader() throws Exception {

        if (publicSelChImplLoadedInBootstrapClassLoader) return;

        publicSelChImplLoadedInBootstrapClassLoader = true;

        //if (getVersion() >= 9) return;

        {
            InputStream is = CompatSniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream("META-INF/bytecode/sun/nio/ch/DatagramChannelDelegate.class");
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
                    "sun.nio.ch.DatagramChannelDelegate",
                    baos.toByteArray(),
                    0,
                    baos.size(),
                    null,
                    null
            );

            Class.forName("sun.nio.ch.DatagramChannelDelegate");
        }

        {
            InputStream is = CompatSniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream("META-INF/bytecode/sun/nio/ch/SocketChannelDelegate.class");
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
                    "sun.nio.ch.SocketChannelDelegate",
                    baos.toByteArray(),
                    0,
                    baos.size(),
                    null,
                    null
            );

            Class.forName("sun.nio.ch.SocketChannelDelegate");
        }

        {
            InputStream is = CompatSniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream("META-INF/bytecode/sun/nio/ch/ServerSocketChannelDelegate.class");
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
                    "sun.nio.ch.ServerSocketChannelDelegate",
                    baos.toByteArray(),
                    0,
                    baos.size(),
                    null,
                    null
            );

            Class.forName("sun.nio.ch.ServerSocketChannelDelegate");
        }

        {
            InputStream is = CompatSniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream("META-INF/bytecode/sun/nio/ch/PipeSinkChannelDelegate.class");
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
                    "sun.nio.ch.PipeSinkChannelDelegate",
                    baos.toByteArray(),
                    0,
                    baos.size(),
                    null,
                    null
            );

            Class.forName("sun.nio.ch.PipeSinkChannelDelegate");
        }

        {
            InputStream is = CompatSniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream("META-INF/bytecode/sun/nio/ch/PipeSourceChannelDelegate.class");
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
                    "sun.nio.ch.PipeSourceChannelDelegate",
                    baos.toByteArray(),
                    0,
                    baos.size(),
                    null,
                    null
            );

            Class.forName("sun.nio.ch.PipeSourceChannelDelegate");
        }

        if (getVersion() < 7) {

            {
                InputStream is = CompatSniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream("META-INF/bytecode/java/net/SocketOption.class");
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
                        "java.net.SocketOption",
                        baos.toByteArray(),
                        0,
                        baos.size(),
                        null,
                        null
                );

                Class.forName("java.net.SocketOption");
            }

            {
                InputStream is = CompatSniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream("META-INF/bytecode/java/nio/channels/NetworkChannel.class");
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
                        "java.nio.channels.NetworkChannel",
                        baos.toByteArray(),
                        0,
                        baos.size(),
                        null,
                        null
                );

                Class.forName("java.nio.channels.NetworkChannel");
            }

            {
                InputStream is = CompatSniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream("META-INF/bytecode/java/net/ProtocolFamily.class");
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
                        "java.net.ProtocolFamily",
                        baos.toByteArray(),
                        0,
                        baos.size(),
                        null,
                        null
                );

                Class.forName("java.net.ProtocolFamily");
            }

        }

    }

    public static void initialize() {

        try {
            loadPublicSelChImplInBootstrapClassLoader();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
