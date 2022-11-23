package io.sniffy.socket;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.Unsafe;
import io.sniffy.reflection.UnsafeInvocationException;
import io.sniffy.reflection.constructor.UnresolvedZeroArgsClassConstructorRef;
import io.sniffy.reflection.field.UnresolvedStaticFieldRef;
import io.sniffy.reflection.method.UnresolvedStaticNonVoidMethodRef;
import io.sniffy.util.ExceptionUtil;
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

    private static final Polyglog LOG = PolyglogFactory.log(SnifferSocketImplFactory.class);

    private static final Polyglog CONSTRUCTOR_VERBOSE_LOG = PolyglogFactory.oneTimeLog(SnifferSocketImplFactory.class);

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

            UnresolvedStaticFieldRef<SocketImplFactory> factoryFieldRef = $(Socket.class).getStaticField("factory");
            SocketImplFactory currentSocketImplFactory = factoryFieldRef.get();

            LOG.info("Original SocketImplFactory was " + currentSocketImplFactory);

            if (null == currentSocketImplFactory || !SnifferSocketImplFactory.class.equals(currentSocketImplFactory.getClass())) {
                factoryFieldRef.set(null);
                SnifferSocketImplFactory snifferSocketImplFactory = new SnifferSocketImplFactory();
                Socket.setSocketImplFactory(snifferSocketImplFactory);

                LOG.info("SocketImplFactory set to " + snifferSocketImplFactory);

                previousSocketImplFactory = currentSocketImplFactory;
            }
        } catch (UnresolvedRefException e) {
            LOG.error(e);
            throw new IOException("Failed to initialize SnifferSocketImplFactory", e);
        } catch (UnsafeInvocationException e) {
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
            UnresolvedStaticFieldRef<SocketImplFactory> factoryFieldRef = $(Socket.class).getStaticField("factory");
            factoryFieldRef.set(previousSocketImplFactory);
        } catch (UnresolvedRefException e) {
            LOG.error(e);
            throw new IOException("Failed to uninstall SnifferSocketImplFactory", e);
        } catch (UnsafeInvocationException e) {
            LOG.error(e);
            throw new IOException("Failed to uninstall SnifferSocketImplFactory", e);
        }

    }

    @Override
    public SocketImpl createSocketImpl() {
        SocketImpl socketImpl = isServerSocketAccept() ? newSocketImpl(false) :
                isServerSocket() ? newSocketImpl(true) :
                        Unsafe.getJavaVersion() > 6 ? new SnifferSocketImpl(newSocketImpl(false)) :
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

        SocketImpl originalSocketImpl = null;

        if (null != previousSocketImplFactory) {
            LOG.trace("Creating SocketImpl delegate using original SocketImplFactory " + previousSocketImplFactory);
            originalSocketImpl = previousSocketImplFactory.createSocketImpl();
        }

        if (createPlatformSocketImplMethodRef.isResolved()) {
            try {
                LOG.trace("Creating SocketImpl delegate using original SocketImpl factory method " + createPlatformSocketImplMethodRef + " with argument serverSocket=" + serverSocket);
                originalSocketImpl = createPlatformSocketImplMethodRef.invoke(serverSocket);
            } catch (Exception e) {
                LOG.error(e);
                throw ExceptionUtil.throwException(e);
            }
        }

        if (defaultSocksSocketImplClassConstructor.isResolved()) {
            try {
                LOG.trace("Creating SocketImpl delegate using original SocketImpl constructor " + defaultSocksSocketImplClassConstructor);
                originalSocketImpl = defaultSocksSocketImplClassConstructor.newInstance();
            } catch (UnresolvedRefException e) {
                LOG.error(e);
                throw ExceptionUtil.throwException(e);
            } catch (UnsafeInvocationException e) {
                LOG.error(e);
                throw ExceptionUtil.throwException(e);
            }
        }

        /*if (null != defaultSocketImplFactoryMethod) {
            //noinspection TryWithIdenticalCatches
            try {
                LOG.trace("Creating SocketImpl delegate using original SocketImpl factory method " + defaultSocketImplFactoryMethod + " with argument serverSocket=" + serverSocket);
                originalSocketImpl = (SocketImpl) defaultSocketImplFactoryMethod.invoke(null, serverSocket);
            } catch (IllegalAccessException e) {
                LOG.error(e);
                throw ExceptionUtil.throwException(e);
            } catch (InvocationTargetException e) {
                LOG.error(e);
                throw ExceptionUtil.throwException(e);
            }
        }*/

        return originalSocketImpl;

    }

    //@SuppressWarnings("unchecked")
    /*private static Class<? extends SocketImpl> getDefaultSocketImplClass() throws ClassNotFoundException {
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
    }*/

}
