package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.OSUtil;
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
public class SniffySelectorProvider extends SelectorProvider {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelectorProvider.class);

    private static final Object INSTALLATION_LOCK = new Object();

    private static volatile SelectorProvider previousSelectorProvider;
    private static volatile NioInstallationResult lastInstallationResult = NioInstallationResult.of(
            NioInstallationResult.Status.UNINSTALLED, "NIO provider has not been installed");
    private static boolean unsupportedPlatformLogged;

    private final SelectorProvider delegate;

    public SniffySelectorProvider(SelectorProvider delegate) {
        this.delegate = delegate;
    }

    public static boolean install() {
        return installWithResult().isInstalled();
    }

    public static NioInstallationResult installWithResult() {
        synchronized (INSTALLATION_LOCK) {
            final JdkNioAccess access;
            try {
                access = JdkNioAccess.resolve();
            } catch (JdkNioAccess.JdkNioAccessException e) {
                lastInstallationResult = NioInstallationResult.of(NioInstallationResult.Status.UNSUPPORTED,
                        e.getMessage(), e);
                if (!unsupportedPlatformLogged) {
                    LOG.error("NIO monitoring is unsupported on this runtime; classic socket monitoring remains active", e);
                    unsupportedPlatformLogged = true;
                }
                return lastInstallationResult;
            }

            SelectorProvider delegate = access.getSelectorProvider();
            if (delegate == null) {
                lastInstallationResult = NioInstallationResult.of(NioInstallationResult.Status.FAILED,
                        "JDK SelectorProvider slot " + access.describeProviderSlot() + " is null");
                LOG.error(lastInstallationResult.getMessage());
                return lastInstallationResult;
            }
            if (delegate instanceof SniffySelectorProvider) {
                SniffySelectorProvider installed = (SniffySelectorProvider) delegate;
                if (previousSelectorProvider == null) {
                    previousSelectorProvider = installed.delegate;
                }
                lastInstallationResult = NioInstallationResult.of(NioInstallationResult.Status.ALREADY_INSTALLED,
                        "Sniffy SelectorProvider is already installed");
                return lastInstallationResult;
            }

            SniffySelectorProvider wrapper = new SniffySelectorProvider(delegate);
            try {
                access.setSelectorProvider(wrapper);
                if (access.getSelectorProvider() != wrapper) {
                    access.setSelectorProvider(delegate);
                    throw new IllegalStateException("JDK SelectorProvider slot did not retain the Sniffy provider");
                }
                previousSelectorProvider = delegate;
                unsupportedPlatformLogged = false;
                lastInstallationResult = NioInstallationResult.of(NioInstallationResult.Status.INSTALLED,
                        "Installed Sniffy SelectorProvider around " + delegate.getClass().getName());
                LOG.info(lastInstallationResult.getMessage());
                return lastInstallationResult;
            } catch (Throwable e) {
                try {
                    access.setSelectorProvider(delegate);
                } catch (Throwable rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
                lastInstallationResult = NioInstallationResult.of(NioInstallationResult.Status.FAILED,
                        "Failed to install Sniffy SelectorProvider; the original provider was retained", e);
                LOG.error(lastInstallationResult.getMessage(), e);
                return lastInstallationResult;
            }
        }
    }

    public static boolean uninstall() {
        return uninstallWithResult().getStatus() == NioInstallationResult.Status.UNINSTALLED;
    }

    public static NioInstallationResult uninstallWithResult() {
        synchronized (INSTALLATION_LOCK) {
            if (previousSelectorProvider == null) {
                lastInstallationResult = NioInstallationResult.of(NioInstallationResult.Status.UNINSTALLED,
                        "No Sniffy SelectorProvider installation is active");
                return lastInstallationResult;
            }
            try {
                JdkNioAccess access = JdkNioAccess.resolve();
                SelectorProvider current = access.getSelectorProvider();
                if (!(current instanceof SniffySelectorProvider)) {
                    lastInstallationResult = NioInstallationResult.of(NioInstallationResult.Status.FAILED,
                            "Cannot uninstall Sniffy NIO provider because the global provider was replaced by " + current);
                    LOG.error(lastInstallationResult.getMessage());
                    return lastInstallationResult;
                }
                access.setSelectorProvider(previousSelectorProvider);
                if (access.getSelectorProvider() != previousSelectorProvider) {
                    throw new IllegalStateException("JDK SelectorProvider slot did not retain the original provider");
                }
                previousSelectorProvider = null;
                lastInstallationResult = NioInstallationResult.of(NioInstallationResult.Status.UNINSTALLED,
                        "Restored the original SelectorProvider");
                LOG.info(lastInstallationResult.getMessage());
                return lastInstallationResult;
            } catch (Throwable e) {
                lastInstallationResult = NioInstallationResult.of(NioInstallationResult.Status.FAILED,
                        "Failed to restore the original SelectorProvider", e);
                LOG.error(lastInstallationResult.getMessage(), e);
                return lastInstallationResult;
            }
        }
    }

    public static NioInstallationResult getLastInstallationResult() {
        return lastInstallationResult;
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
    @SuppressWarnings({"unused", "RedundantThrows"})
    public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        try {
            return new SniffyServerSocketChannel(this,
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
