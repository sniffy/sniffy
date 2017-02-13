package io.sniffy.socket;

import io.sniffy.util.ExceptionUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;

public class SnifferSocketImplFactory implements SocketImplFactory {

    protected final static Constructor<? extends SocketImpl> defaultSocketImplClassConstructor =
            getDefaultSocketImplClassConstructor();

    private volatile static SocketImplFactory previousSocketImplFactory;

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
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    public static void uninstall() {
        try {
            Field factoryField = Socket.class.getDeclaredField("factory");
            factoryField.setAccessible(true);
            factoryField.set(null, previousSocketImplFactory);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
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
