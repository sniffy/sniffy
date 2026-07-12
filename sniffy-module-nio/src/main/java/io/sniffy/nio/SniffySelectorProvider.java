package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
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
    private static final ThreadLocal<Integer> DELEGATE_SELECTOR_CONSTRUCTION_DEPTH = new ThreadLocal<Integer>();

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
        // UDP is deliberately pass-through until Sniffy's connection model can represent datagram endpoints.
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
        return isDelegateSelectorConstruction() ?
                delegate.openPipe() :
                new SniffyPipe(this, delegate.openPipe());
    }

    @Override
    public AbstractSelector openSelector() throws IOException {
        enterDelegateSelectorConstruction();
        try {
            return new SniffySelector(this, delegate.openSelector());
        } finally {
            exitDelegateSelectorConstruction();
        }
    }

    /** @return a monitored server channel, except during scoped delegate-selector construction. */
    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return isDelegateSelectorConstruction() ?
                delegate.openServerSocketChannel() :
                new SniffyServerSocketChannel(this, delegate.openServerSocketChannel());
    }

    /** @return a monitored socket channel, except during scoped delegate-selector construction. */
    @Override
    public SocketChannel openSocketChannel() throws IOException {
        return isDelegateSelectorConstruction() ?
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
        } else {
            return channel;
        }
    }

    // Note: this method was absent in earlier JDKs (15-) so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"unused", "RedundantThrows"})
    public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        try {
            return isDelegateSelectorConstruction() ?
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
            return isDelegateSelectorConstruction() ?
                    invokeMethod(SelectorProvider.class, delegate, "openServerSocketChannel",
                            ProtocolFamily.class, family,
                            ServerSocketChannel.class) :
                    new SniffyServerSocketChannel(this,
                    invokeMethod(SelectorProvider.class, delegate, "openServerSocketChannel",
                        ProtocolFamily.class, family,
                        ServerSocketChannel.class
                )
            );
        } catch (Exception e) {
            throw processException(e);
        }
    }

    static boolean isDelegateSelectorConstruction() {
        Integer depth = DELEGATE_SELECTOR_CONSTRUCTION_DEPTH.get();
        return null != depth && depth > 0;
    }

    private static void enterDelegateSelectorConstruction() {
        Integer depth = DELEGATE_SELECTOR_CONSTRUCTION_DEPTH.get();
        DELEGATE_SELECTOR_CONSTRUCTION_DEPTH.set(null == depth ? 1 : depth + 1);
    }

    private static void exitDelegateSelectorConstruction() {
        Integer depth = DELEGATE_SELECTOR_CONSTRUCTION_DEPTH.get();
        if (null == depth || depth <= 1) {
            DELEGATE_SELECTOR_CONSTRUCTION_DEPTH.remove();
        } else {
            DELEGATE_SELECTOR_CONSTRUCTION_DEPTH.set(depth - 1);
        }
    }

}
