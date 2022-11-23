package io.sniffy.nio.compat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.sniffy.reflection.Unsafe.getJavaVersion;

/**
 * @since 3.1.7
 */
@SuppressWarnings("unused")
public class CompatSniffySelectorProviderBootstrap {

    private static boolean publicSelChImplLoadedInBootstrapClassLoader = false;

    public static void loadPublicSelChImplInBootstrapClassLoader() throws Exception {

        if (publicSelChImplLoadedInBootstrapClassLoader) return;

        publicSelChImplLoadedInBootstrapClassLoader = true;

        //if (getVersion() >= 9) return;

        {
            byte[] bytes = loadResource("META-INF/bytecode/sun/nio/ch/DatagramChannelDelegate.class");
            io.sniffy.reflection.Unsafe.defineSystemClass("sun.nio.ch.DatagramChannelDelegate", bytes);
            Class.forName("sun.nio.ch.DatagramChannelDelegate");
        }

        {
            byte[] bytes = loadResource("META-INF/bytecode/sun/nio/ch/SocketChannelDelegate.class");
            io.sniffy.reflection.Unsafe.defineSystemClass("sun.nio.ch.SocketChannelDelegate", bytes);
            Class.forName("sun.nio.ch.SocketChannelDelegate");
        }

        {
            byte[] bytes = loadResource("META-INF/bytecode/sun/nio/ch/ServerSocketChannelDelegate.class");
            io.sniffy.reflection.Unsafe.defineSystemClass("sun.nio.ch.ServerSocketChannelDelegate", bytes);
            Class.forName("sun.nio.ch.ServerSocketChannelDelegate");
        }

        {
            byte[] bytes = loadResource("META-INF/bytecode/sun/nio/ch/PipeSinkChannelDelegate.class");
            io.sniffy.reflection.Unsafe.defineSystemClass("sun.nio.ch.PipeSinkChannelDelegate", bytes);
            Class.forName("sun.nio.ch.PipeSinkChannelDelegate");
        }

        {
            byte[] bytes = loadResource("META-INF/bytecode/sun/nio/ch/PipeSourceChannelDelegate.class");
            io.sniffy.reflection.Unsafe.defineSystemClass("sun.nio.ch.PipeSourceChannelDelegate", bytes);
            Class.forName("sun.nio.ch.PipeSourceChannelDelegate");
        }

        if (getJavaVersion() < 7) {

            {
                byte[] bytes = loadResource("META-INF/bytecode/java/net/SocketOption.class");
                io.sniffy.reflection.Unsafe.defineSystemClass("java.net.SocketOption", bytes);
                Class.forName("java.net.SocketOption");
            }

            {
                byte[] bytes = loadResource("META-INF/bytecode/java/nio/channels/NetworkChannel.class");
                io.sniffy.reflection.Unsafe.defineSystemClass("java.nio.channels.NetworkChannel", bytes);
                Class.forName("java.nio.channels.NetworkChannel");
            }

            {
                byte[] bytes = loadResource("META-INF/bytecode/java/net/ProtocolFamily.class");
                io.sniffy.reflection.Unsafe.defineSystemClass("java.net.ProtocolFamily", bytes);
                Class.forName("java.net.ProtocolFamily");
            }

        }

    }

    private static byte[] loadResource(String path) throws IOException {
        InputStream is = CompatSniffySelectorProviderBootstrap.class.getClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int i;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }

        is.close();
        byte[] bytes = baos.toByteArray();
        return bytes;
    }

    public static void initialize() {

        try {
            loadPublicSelChImplInBootstrapClassLoader();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
