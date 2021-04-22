package io.sniffy.socket;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;

/**
 * @since 3.1
 */
public class SnifferSocketImplFactory implements SocketImplFactory {

    private static final Polyglog LOG = PolyglogFactory.log(SnifferSocketImplFactory.class);

    // @VisibleForTesting
    protected final static Constructor<? extends SocketImpl> defaultSocketImplClassConstructor =
            getDefaultSocketImplClassConstructor();

    // @VisibleForTesting
    protected final static Method defaultSocketImplFactoryMethod =
            getDefaultSocketImplFactoryMethod();

    private volatile static SocketImplFactory previousSocketImplFactory;

    /**
     * Backups the existing {@link SocketImplFactory} and sets the {@link SnifferSocketImplFactory} as a default
     *
     * @throws IOException if failed to install {@link SnifferSocketImplFactory}
     * @see #uninstall()
     * @since 3.1
     */
    public static void install() throws IOException {

        //noinspection TryWithIdenticalCatches
        try {

            SocketImplFactory currentSocketImplFactory = ReflectionUtil.getField(Socket.class, null, "factory");

            LOG.info("Original SocketImplFactory was " + currentSocketImplFactory);

            if (null == currentSocketImplFactory || !SnifferSocketImplFactory.class.equals(currentSocketImplFactory.getClass())) {
                if (!ReflectionUtil.setField(Socket.class, null, "factory", null)) {
                    throw new IOException("Failed to uninstall SnifferSocketImplFactory");
                }
                SnifferSocketImplFactory snifferSocketImplFactory = new SnifferSocketImplFactory();
                Socket.setSocketImplFactory(snifferSocketImplFactory);

                LOG.info("SocketImplFactory set to " + snifferSocketImplFactory);

                previousSocketImplFactory = currentSocketImplFactory;
            }
        } catch (IllegalAccessException e) {
            LOG.error(e);
            throw new IOException("Failed to initialize SnifferSocketImplFactory", e);
        } catch (NoSuchFieldException e) {
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

        if (!ReflectionUtil.setField(Socket.class, null, "factory", previousSocketImplFactory)) {
            LOG.error("Couldn't uninstall Sniffy SocketImplFactory");
            throw new IOException("Failed to uninstall SnifferSocketImplFactory");
        }
    }

    @Override
    public SocketImpl createSocketImpl() {
        SocketImpl socketImpl = isServerSocketAccept() ? newSocketImpl(false) :
                isServerSocket() ? newSocketImpl(true) :
                        JVMUtil.getVersion() > 6 ? new SnifferSocketImpl(newSocketImpl(false)) :
                                new CompatSnifferSocketImpl(newSocketImpl(false));
        LOG.trace("Created SocketImpl " + socketImpl);
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

        SocketImpl originalSocketImpl = null;

        if (null != previousSocketImplFactory) {
            LOG.trace("Creating SocketImpl delegate using original SocketImplFactory " + previousSocketImplFactory);
            originalSocketImpl = previousSocketImplFactory.createSocketImpl();
        }

        if (null != defaultSocketImplClassConstructor) {
            try {
                LOG.trace("Creating SocketImpl delegate using original SocketImpl constructor " + defaultSocketImplClassConstructor);
                originalSocketImpl = defaultSocketImplClassConstructor.newInstance();
            } catch (Exception e) {
                LOG.error(e);
                throw ExceptionUtil.throwException(e);
            }
        }

        if (null != defaultSocketImplFactoryMethod) {
            //noinspection TryWithIdenticalCatches
            try {
                LOG.trace("Creating SocketImpl delegate using original SocketImpl factory method " + defaultSocketImplFactoryMethod);
                originalSocketImpl = (SocketImpl) defaultSocketImplFactoryMethod.invoke(null, serverSocket);
            } catch (IllegalAccessException e) {
                LOG.error(e);
                throw ExceptionUtil.throwException(e);
            } catch (InvocationTargetException e) {
                LOG.error(e);
                throw ExceptionUtil.throwException(e);
            }
        }

        return originalSocketImpl;

    }

    @SuppressWarnings("unchecked")
    private static Class<? extends SocketImpl> getDefaultSocketImplClass() throws ClassNotFoundException {
        return (Class<? extends SocketImpl>) Class.forName("java.net.SocksSocketImpl");
    }

    private static Constructor<? extends SocketImpl> getDefaultSocketImplClassConstructor() {
        Constructor<? extends SocketImpl> constructor;
        //noinspection TryWithIdenticalCatches
        try {
            constructor = getDefaultSocketImplClass().getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }

        ReflectionUtil.setAccessible(constructor);

        return constructor;
    }

    private static Class<?> getSocketImplClass() throws ClassNotFoundException {
        return Class.forName("java.net.SocketImpl");
    }

    private static Method getDefaultSocketImplFactoryMethod() {
        Method factoryMethod;
        //noinspection TryWithIdenticalCatches
        try {
            factoryMethod = getSocketImplClass().getDeclaredMethod("createPlatformSocketImpl", Boolean.TYPE);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
        ReflectionUtil.setAccessible(factoryMethod);
        return factoryMethod;
    }

}
