package io.sniffy.socket;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionUtil;

import java.io.IOException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.util.Set;

/**
 * @since 3.1
 */
class SnifferSocketImpl extends CompatSnifferSocketImpl {

    protected SnifferSocketImpl(SocketImpl delegate, Sleep sleep) {
        super(delegate, sleep);
    }

    protected SnifferSocketImpl(SocketImpl delegate) {
        this(delegate, new Sleep());
    }

    // New methods in Java 9
    // We cannot have them in parent class since SocketOption isn't available on Java 6

    //@Override
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        long start = System.currentTimeMillis();
        try {
            ReflectionUtil.invokeMethod(SocketImpl.class, delegate, "setOption", SocketOption.class, name, Object.class, value, Void.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    //@Override
    @SuppressWarnings("unchecked")
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        long start = System.currentTimeMillis();
        try {
            return (T) ReflectionUtil.invokeMethod(SocketImpl.class, delegate, "getOption", SocketOption.class, name, Object.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

    //@Override
    @SuppressWarnings("unchecked")
    protected Set<SocketOption<?>> supportedOptions() {
        long start = System.currentTimeMillis();
        try {
            return (Set<SocketOption<?>>) ReflectionUtil.invokeMethod(SocketImpl.class, delegate, "supportedOptions", Set.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

}
