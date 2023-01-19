package io.sniffy.socket;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.Unsafe;
import io.sniffy.reflection.constructor.UnresolvedZeroArgsClassConstructorRef;
import io.sniffy.reflection.field.UnresolvedStaticFieldRef;
import io.sniffy.reflection.method.UnresolvedStaticNonVoidMethodRef;
import io.sniffy.util.StackTraceExtractor;
import io.sniffy.util.StringUtil;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;

import static io.sniffy.reflection.Unsafe.$;

/**
 * @since 3.1
 */
public class SnifferSocketImplFactory implements SocketImplFactory {

    private final static Polyglog LOG = PolyglogFactory.log(SnifferSocketImplFactory.class);

    private final static Polyglog CONSTRUCTOR_VERBOSE_LOG = PolyglogFactory.oneTimeLog(SnifferSocketImplFactory.class);

    protected final static UnresolvedStaticFieldRef<SocketImplFactory> factoryFieldRef = $(Socket.class).getStaticField("factory");

    // @VisibleForTesting
    protected final static UnresolvedZeroArgsClassConstructorRef<? extends SocketImpl> defaultSocksSocketImplClassConstructor =
            $("java.net.SocksSocketImpl", SocketImpl.class).tryGetConstructor();

    // @VisibleForTesting
    protected final static UnresolvedStaticNonVoidMethodRef<SocketImpl> createPlatformSocketImplMethodRef =
            $(SocketImpl.class).getStaticMethod(SocketImpl.class, "createPlatformSocketImpl", Boolean.TYPE);

    private volatile static SocketImplFactory previousSocketImplFactory;

    /**
     * Backups the existing {@link SocketImplFactory} and sets the {@link SnifferSocketImplFactory} as a default
     *
     * @throws IOException if failed to install {@link SnifferSocketImplFactory}
     * @see #uninstall()
     * @since 3.1
     */
    public static void install() throws IOException {
        try {

            assert createPlatformSocketImplMethodRef.isResolved() || defaultSocksSocketImplClassConstructor.isResolved();
            assert factoryFieldRef.isResolved();

            synchronized (Socket.class) {
                SocketImplFactory currentSocketImplFactory = factoryFieldRef.get();
                LOG.info("Original SocketImplFactory was " + currentSocketImplFactory);
                if (null == currentSocketImplFactory || !SnifferSocketImplFactory.class.equals(currentSocketImplFactory.getClass())) {
                    SnifferSocketImplFactory snifferSocketImplFactory = new SnifferSocketImplFactory();
                    LOG.info("SocketImplFactory set to " + snifferSocketImplFactory);
                    factoryFieldRef.set(snifferSocketImplFactory);
                    previousSocketImplFactory = currentSocketImplFactory;
                }
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new IOException("Failed to initialize SnifferSocketImplFactory", e);
        }

    }

    /**
     * Restores previously saved {@link SocketImplFactory} and sets it as a default
     *
     * @throws IOException if failed to install {@link SnifferSocketImplFactory}
     * @see #install()
     * @since 3.1
     */
    public static void uninstall() throws IOException {

        LOG.info("Uninstalling SniffySocketImplFactory - replacing with " + previousSocketImplFactory);

        try {
            synchronized (Socket.class) {
                factoryFieldRef.set(previousSocketImplFactory);
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new IOException("Failed to uninstall SnifferSocketImplFactory", e);
        }

    }

    @Override
    public SocketImpl createSocketImpl() {
        SocketImpl socketImpl = isServerSocketAccept() ? newSocketImpl(false) :
                isServerSocket() ? newSocketImpl(true) :
                        Unsafe.tryGetJavaVersion() > 6 ? new SnifferSocketImpl(newSocketImpl(false)) :
                                new CompatSnifferSocketImpl(newSocketImpl(false));
        LOG.trace("Created SocketImpl " + socketImpl);
        // TODO: optimize polyglog to support lazy evaluation in order not to call StackTraceExtractor.getStackTraceAsString() each time
        CONSTRUCTOR_VERBOSE_LOG.trace("StackTrace for creating new SocketImpl was " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
        return socketImpl;
    }

    private static boolean isServerSocketAccept() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            if (ste.getClassName().startsWith("java.net.ServerSocket") || ste.getClassName().startsWith("sun.security.ssl.SSLServerSocketImpl")) {
                if (ste.getMethodName().equals("accept")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isServerSocket() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            if (ste.getClassName().startsWith("java.net.ServerSocket") || ste.getClassName().startsWith("sun.security.ssl.SSLServerSocketImpl")) {
                return true;
            }
        }
        return false;
    }

    private static SocketImpl newSocketImpl(boolean serverSocket) {

        if (null != previousSocketImplFactory) {
            LOG.trace("Creating SocketImpl delegate using original SocketImplFactory " + previousSocketImplFactory);
            return previousSocketImplFactory.createSocketImpl();
        } else if (createPlatformSocketImplMethodRef.isResolved()) {
            try {
                LOG.trace("Creating SocketImpl delegate using original SocketImpl factory method " + createPlatformSocketImplMethodRef + " with argument serverSocket=" + serverSocket);
                return createPlatformSocketImplMethodRef.invoke(serverSocket);
            } catch (Exception e) {
                LOG.error(e);
                throw Unsafe.throwException(e);
            }
        } else if (defaultSocksSocketImplClassConstructor.isResolved()) {
            try {
                LOG.trace("Creating SocketImpl delegate using original SocketImpl constructor " + defaultSocksSocketImplClassConstructor);
                return defaultSocksSocketImplClassConstructor.newInstance();
            } catch (Exception e) {
                LOG.error(e);
                throw Unsafe.throwException(e);
            }
        } else {
            throw new AssertionError("Couldn't create original SocketImpl instance");
        }

    }

}
