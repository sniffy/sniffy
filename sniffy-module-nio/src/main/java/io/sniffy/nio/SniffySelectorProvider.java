package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.field.UnresolvedStaticFieldRef;
import io.sniffy.util.OSUtil;
import io.sniffy.util.StackTraceExtractor;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

import static io.sniffy.reflection.Unsafe.$;
import static io.sniffy.util.ExceptionUtil.processException;

/**
 * @since 3.1.7
 */
public class SniffySelectorProvider extends SelectorProvider {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelectorProvider.class);

    private static volatile SelectorProvider previousSelectorProvider;

    private final SelectorProvider delegate;

    public SniffySelectorProvider(SelectorProvider delegate) {
        this.delegate = delegate;
    }

    public static synchronized boolean install() {

        SelectorProvider delegate = SelectorProvider.provider();

        LOG.info("Original SelectorProvider was " + delegate);

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

        LOG.info("Setting SelectorProvider to " + sniffySelectorProvider);

        UnresolvedStaticFieldRef<SelectorProvider> instanceFieldRef = $("java.nio.channels.spi.SelectorProvider$Holder").tryGetStaticField("INSTANCE");
        if (instanceFieldRef.isResolved()) {
            return instanceFieldRef.trySet(sniffySelectorProvider);
        } else {
            UnresolvedStaticFieldRef<SelectorProvider> providerFieldRef = $(SelectorProvider.class).getStaticField("provider");
            if (providerFieldRef.isResolved()) {
                return providerFieldRef.trySet(sniffySelectorProvider);
            } else {
                LOG.error("Couldn't initialize SniffySelectorProvider since both java.nio.channels.spi.SelectorProvider$Holder.INSTANCE and java.nio.channels.spi.SelectorProvider.provider are unavailable");
                return false;
            }
        }

    }

    public static boolean uninstall() {

        LOG.info("Restoring original SelectorProvider " + previousSelectorProvider);

        if (null == previousSelectorProvider) {
            return false;
        }

        UnresolvedStaticFieldRef<SelectorProvider> instanceFieldRef = $("java.nio.channels.spi.SelectorProvider$Holder").tryGetStaticField("INSTANCE");
        if (instanceFieldRef.isResolved()) {
            return instanceFieldRef.trySet(previousSelectorProvider);
        } else {
            UnresolvedStaticFieldRef<SelectorProvider> providerFieldRef = $(SelectorProvider.class).getStaticField("provider");
            if (providerFieldRef.isResolved()) {
                return providerFieldRef.trySet(previousSelectorProvider);
            } else {
                LOG.error("Couldn't restore original SelectorProvider since both java.nio.channels.spi.SelectorProvider$Holder.INSTANCE and java.nio.channels.spi.SelectorProvider.provider are unavailable");
                return false;
            }
        }

    }

    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
        return new SniffyDatagramChannelAdapter(this, delegate.openDatagramChannel());
    }

    // Available in Java 1.7+ only
    @Override
    @IgnoreJRERequirement
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        return new SniffyDatagramChannelAdapter(this, delegate.openDatagramChannel(family));
    }

    @Override
    public Pipe openPipe() throws IOException {
        // TODO: can we handle it better?
        return OSUtil.isWindows() && StackTraceExtractor.hasClassAndMethodInStackTrace("io.sniffy.nio.SniffySelectorProvider", "openSelector") ?
                delegate.openPipe() :
                new SniffyPipe(this, delegate.openPipe());
    }

    @Override
    public AbstractSelector openSelector() throws IOException {
        return new SniffySelector(this, delegate.openSelector());
    }

    /**
     * @return a Sniffy Wrapper around SocketChannel unless we're on Windows and SocketChannel is created for Pipe
     * @throws IOException on underlying IOException
     */
    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        // TODO: can we handle it better?
        return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                delegate.openServerSocketChannel() :
                new SniffyServerSocketChannel(this, delegate.openServerSocketChannel());
    }

    /**
     * @return a Sniffy Wrapper around SocketChannel unless we're on Windows and SocketChannel is created for Pipe
     * @throws IOException on underlying IOException
     */
    @Override
    public SocketChannel openSocketChannel() throws IOException {
        // TODO: can we handle it better?
        return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                delegate.openSocketChannel() :
                new SniffySocketChannel(this, delegate.openSocketChannel());
    }

    @Override
    public Channel inheritedChannel() throws IOException {
        Channel channel = delegate.inheritedChannel();
        if (channel instanceof SocketChannel) {
            return new SniffySocketChannel(this, (SocketChannel) channel);
        } else if (channel instanceof ServerSocketChannel) {
            return new SniffyServerSocketChannel(this, (ServerSocketChannel) channel);
        } else if (channel instanceof DatagramChannel) {
            return new SniffyDatagramChannelAdapter(this, (DatagramChannel) channel);
        } else {
            return channel;
        }
    }

    // Note: this method was absent in earlier JDKs (15-) so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"unused", "RedundantThrows"})
    public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        try {
            // TODO: can we handle it better?
            return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                    $(SelectorProvider.class).getNonStaticMethod(SocketChannel.class, "openSocketChannel", ProtocolFamily.class).invoke(delegate, family)
                    :
                    new SniffySocketChannel(
                            this,
                            $(SelectorProvider.class).getNonStaticMethod(SocketChannel.class, "openSocketChannel", ProtocolFamily.class).invoke(delegate, family)
                    );
        } catch (Exception e) {
            throw processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs (15-) so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"unused", "RedundantThrows"})
    public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        try {
            // TODO: shall we check for Pipe in stacktrace as well?
            return new SniffyServerSocketChannel(this,
                $(SelectorProvider.class).getNonStaticMethod(ServerSocketChannel.class, "openServerSocketChannel", ProtocolFamily.class).invoke(delegate, family)
            );
        } catch (Exception e) {
            throw processException(e);
        }
    }

}
