package io.sniffy.socket;

import io.sniffy.util.ExceptionUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;

/**
 * @since 3.1
 */
public class SnifferSocketImplFactory implements SocketImplFactory {

    protected final static Constructor<? extends SocketImpl> defaultSocketImplClassConstructor =
            getDefaultSocketImplClassConstructor();

    private volatile static SocketImplFactory previousSocketImplFactory;

    /**
     * Backups the existing {@link SocketImplFactory} and sets the {@link SnifferSocketImplFactory} as a default
     * @see #uninstall()
     * @throws IOException if failed to install {@link SnifferSocketImplFactory}
     * @since 3.1
     */
    public static void install() throws IOException {
        
        try {
            Field factoryField = Socket.class.getDeclaredField("factory");
            factoryField.setAccessible(true);
            SocketImplFactory currentSocketImplFactory = (SocketImplFactory) factoryField.get(null);
            if (null == currentSocketImplFactory || !SnifferSocketImplFactory.class.equals(currentSocketImplFactory.getClass())) {
                factoryField.set(null, null);
                Socket.setSocketImplFactory(new SnifferSocketImplFactory());
                previousSocketImplFactory = currentSocketImplFactory;
            }
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to initialize SnifferSocketImplFactory", e);
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to initialize SnifferSocketImplFactory", e);
        }

    }

    /**
     * Restores previously saved {@link SocketImplFactory} and sets it as a default
     * @see #install()
     * @throws IOException if failed to install {@link SnifferSocketImplFactory}
     * @since 3.1
     */
    public static void uninstall() throws IOException {
        try {
            Field factoryField = Socket.class.getDeclaredField("factory");
            factoryField.setAccessible(true);
            factoryField.set(null, previousSocketImplFactory);
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to initialize SnifferSocketImplFactory", e);
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to initialize SnifferSocketImplFactory", e);
        }
    }

    @Override
    public SocketImpl createSocketImpl() {
        return isServerSocket() ? newSocketImpl() : new SnifferSocketImpl(newSocketImpl());
    }

    private static boolean isServerSocket() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (null != stackTrace) {
            for (StackTraceElement ste : stackTrace) {
                if (ste.getClassName().startsWith("java.net.ServerSocket")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static SocketImpl newSocketImpl() {

        if (null != previousSocketImplFactory) {
            return previousSocketImplFactory.createSocketImpl();
        } else {
            try {
                return null == defaultSocketImplClassConstructor ? null :
                        defaultSocketImplClassConstructor.newInstance();
            } catch (Exception e) {
                ExceptionUtil.throwException(e);
                return null;
            }
        }

    }


    @SuppressWarnings("unchecked")
    private static Class<? extends SocketImpl> getDefaultSocketImplClass() throws ClassNotFoundException {
        return (Class<? extends SocketImpl>) Class.forName("java.net.SocksSocketImpl");
    }

    private static Constructor<? extends SocketImpl> getDefaultSocketImplClassConstructor() {
        Constructor<? extends SocketImpl> constructor;
        try {
            constructor = getDefaultSocketImplClass().getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
        constructor.setAccessible(true);
        return constructor;
    }

}
