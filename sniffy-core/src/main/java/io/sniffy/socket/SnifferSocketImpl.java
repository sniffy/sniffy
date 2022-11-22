package io.sniffy.socket;

import io.sniffy.util.ExceptionUtil;

import java.io.IOException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.util.Set;

import static io.sniffy.reflection.Unsafe.$;

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
            $(SocketImpl.class).method("setOption", SocketOption.class, Object.class).invoke(delegate, name, value);
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
            return (T) $(SocketImpl.class).method(Object.class, "getOption", SocketOption.class).invoke(delegate, name);
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
            return $(SocketImpl.class).method(Set.class, "supportedOptions").invoke(delegate);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            logSocket(System.currentTimeMillis() - start);
        }
    }

}
