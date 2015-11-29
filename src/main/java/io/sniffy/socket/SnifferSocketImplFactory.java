package io.sniffy.socket;

import io.sniffy.util.ExceptionUtil;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;

public class SnifferSocketImplFactory implements SocketImplFactory {

    private final static Constructor<? extends SocketImpl> defaultSocketImplClassConstructor =
            getDefaultSocketImplClassConstructor();

    public static void install() throws IOException {
        // todo: handle previous instance of socket impl factory
        // todo: consider using Unsafe for store fences
        Socket.setSocketImplFactory(new SnifferSocketImplFactory());
    }

    public static void uninstall() {
        try {
            Field factoryField = Socket.class.getDeclaredField("factory");
            factoryField.setAccessible(true);
            factoryField.set(null, null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SocketImpl createSocketImpl() {
        return new SnifferSocketImpl(newSocketImpl());
    }

    private static SocketImpl newSocketImpl() {
        try {
            return null == defaultSocketImplClassConstructor ? null :
                    defaultSocketImplClassConstructor.newInstance();
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
            return null;
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
