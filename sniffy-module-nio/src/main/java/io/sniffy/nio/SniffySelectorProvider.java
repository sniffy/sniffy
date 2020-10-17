package io.sniffy.nio;

import io.sniffy.util.OSUtil;
import io.sniffy.util.ReflectionUtil;
import io.sniffy.util.StackTraceExtractor;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

import static io.sniffy.util.ExceptionUtil.processException;
import static io.sniffy.util.ReflectionUtil.invokeMethod;

public class SniffySelectorProvider extends SelectorProvider {

    private static volatile SelectorProvider previousSelectorProvider;

    private final SelectorProvider delegate;

    public SniffySelectorProvider(SelectorProvider delegate) {
        this.delegate = delegate;
    }

    public static synchronized boolean install() {

        SelectorProvider delegate = SelectorProvider.provider();

        if (null == delegate) {
            return false;
        }

        if (null == previousSelectorProvider && !SniffySelectorProvider.class.equals(delegate.getClass())) {
            previousSelectorProvider = delegate;
        }

        if (SniffySelectorProvider.class.equals(delegate.getClass())) {
            return true;
        }

        SelectorProvider sniffySelectorProvider = new SniffySelectorProvider(delegate);

        if (ReflectionUtil.setField("java.nio.channels.spi.SelectorProvider$Holder", null, "INSTANCE", sniffySelectorProvider)) {
            return true;
        } else {
            return ReflectionUtil.setField(SelectorProvider.class, null, "provider", sniffySelectorProvider, "lock");
        }

    }

    public static boolean uninstall() {

        if (null == previousSelectorProvider) {
            return false;
        }

        if (ReflectionUtil.setField("java.nio.channels.spi.SelectorProvider$Holder", null, "INSTANCE", previousSelectorProvider)) {
            return true;
        } else {
            return ReflectionUtil.setField(SelectorProvider.class, null, "provider", previousSelectorProvider, "lock");
        }

    }

    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
        return delegate.openDatagramChannel();
    }

    // Available in Java 1.7+ only
    @Override
    @IgnoreJRERequirement
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        return delegate.openDatagramChannel(family);
    }

    @Override
    public Pipe openPipe() throws IOException {
        return delegate.openPipe();
    }

    @Override
    public AbstractSelector openSelector() throws IOException {
        return new SniffySelector(this, delegate.openSelector());
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return delegate.openServerSocketChannel();
    }

    /**
     * @return a Sniffy Wrapper around SocketChannel unless we're on Windows and SocketChannel is created for Pipe
     * @throws IOException on underlying IOException
     */
    @Override
    public SocketChannel openSocketChannel() throws IOException {
        return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                delegate.openSocketChannel() :
                new SniffySocketChannel(this, delegate.openSocketChannel());
    }

    @Override
    public Channel inheritedChannel() throws IOException {
        return delegate.inheritedChannel();
    }

    // Note: this method was absent in earlier JDKs (15-) so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("unused")
    public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        try {
            return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                    invokeMethod(SelectorProvider.class, delegate, "openSocketChannel",
                            ProtocolFamily.class, family,
                            SocketChannel.class
                    ) :
                    new SniffySocketChannel(
                            this,
                            invokeMethod(SelectorProvider.class, delegate, "openSocketChannel",
                                    ProtocolFamily.class, family,
                                    SocketChannel.class
                            )
                    );
        } catch (Exception e) {
            throw processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs (15-) so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("unused")
    public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        try {
            return invokeMethod(SelectorProvider.class, delegate, "openServerSocketChannel",
                    ProtocolFamily.class, family,
                    ServerSocketChannel.class
            );
        } catch (Exception e) {
            throw processException(e);
        }
    }

}
