package io.sniffy.nio.compat;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
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

/**
 * @since 3.1.7
 */
public class CompatSniffySelectorProvider extends SelectorProvider {

    private static final Polyglog LOG = PolyglogFactory.log(CompatSniffySelectorProvider.class);

    private static volatile SelectorProvider previousSelectorProvider;

    private final SelectorProvider delegate;

    public CompatSniffySelectorProvider(SelectorProvider delegate) {
        this.delegate = delegate;
    }

    public static synchronized boolean install() {

        SelectorProvider delegate = SelectorProvider.provider();

        LOG.info("Original SelectorProvider was " + delegate);

        if (null == delegate) {
            return false;
        }

        if (null == previousSelectorProvider && !CompatSniffySelectorProvider.class.equals(delegate.getClass())) {
            previousSelectorProvider = delegate;
        }

        if (CompatSniffySelectorProvider.class.equals(delegate.getClass())) {
            return true;
        }

        SelectorProvider sniffySelectorProvider = new CompatSniffySelectorProvider(delegate);

        LOG.info("Setting SelectorProvider to " + sniffySelectorProvider);

        if (ReflectionUtil.setField("java.nio.channels.spi.SelectorProvider$Holder", null, "INSTANCE", sniffySelectorProvider)) {
            return true;
        } else {
            return ReflectionUtil.setField(SelectorProvider.class, null, "provider", sniffySelectorProvider, "lock");
        }

    }

    public static boolean uninstall() {

        LOG.info("Restoring original SelectorProvider " + previousSelectorProvider);

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
        return new CompatSniffyDatagramChannelAdapter(this, delegate.openDatagramChannel());
    }

    // Available in Java 1.7+ only
    // @Override
    @IgnoreJRERequirement
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        return new CompatSniffyDatagramChannelAdapter(this, delegate.openDatagramChannel(family));
    }

    @Override
    public Pipe openPipe() throws IOException {
        return OSUtil.isWindows() && StackTraceExtractor.hasClassAndMethodInStackTrace("io.sniffy.nio.compat.CompatSniffySelectorProvider", "openSelector") ?
                delegate.openPipe() :
                new CompatSniffyPipe(this, delegate.openPipe());
    }

    @Override
    public AbstractSelector openSelector() throws IOException {
        return new CompatSniffySelector(this, delegate.openSelector());
    }

    /**
     * @return a Sniffy Wrapper around SocketChannel unless we're on Windows and SocketChannel is created for Pipe
     * @throws IOException on underlying IOException
     */
    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                delegate.openServerSocketChannel() :
                new CompatSniffyServerSocketChannel(this, delegate.openServerSocketChannel());
    }

    /**
     * @return a Sniffy Wrapper around SocketChannel unless we're on Windows and SocketChannel is created for Pipe
     * @throws IOException on underlying IOException
     */
    @Override
    public SocketChannel openSocketChannel() throws IOException {
        return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                delegate.openSocketChannel() :
                new CompatSniffySocketChannel(this, delegate.openSocketChannel());
    }

    @Override
    public Channel inheritedChannel() throws IOException {
        Channel channel = delegate.inheritedChannel();
        if (channel instanceof SocketChannel) {
            return new CompatSniffySocketChannel(this, (SocketChannel) channel);
        } else if (channel instanceof ServerSocketChannel) {
            return new CompatSniffyServerSocketChannel(this, (ServerSocketChannel) channel);
        } else if (channel instanceof DatagramChannel) {
            return new CompatSniffyDatagramChannelAdapter(this, (DatagramChannel) channel);
        } else {
            return channel;
        }
    }

    //@Override
    @SuppressWarnings({"unused", "RedundantThrows"})
    public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        try {
            return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                    invokeMethod(SelectorProvider.class, delegate, "openSocketChannel",
                            ProtocolFamily.class, family,
                            SocketChannel.class
                    ) :
                    new CompatSniffySocketChannel(
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

    //@Override
    @SuppressWarnings({"unused", "RedundantThrows"})
    public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        try {
            return new CompatSniffyServerSocketChannel(this,
                    invokeMethod(SelectorProvider.class, delegate, "openServerSocketChannel",
                        ProtocolFamily.class, family,
                        ServerSocketChannel.class
                )
            );
        } catch (Exception e) {
            throw processException(e);
        }
    }

}
