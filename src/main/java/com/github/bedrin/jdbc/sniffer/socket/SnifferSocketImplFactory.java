package com.github.bedrin.jdbc.sniffer.socket;

import com.github.bedrin.jdbc.sniffer.util.ExceptionUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;

public class SnifferSocketImplFactory implements SocketImplFactory {

    private final static Constructor<? extends SocketImpl> defaultSocketImplClassConstructor =
            getDefaultSocketImplClassConstructor();

    public static void install() throws IOException {
        Socket.setSocketImplFactory(new SnifferSocketImplFactory());
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
