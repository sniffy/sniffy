package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Legacy compatibility type. Sniffy does not support NIO2/AIO monitoring and never installs this provider.
 *
 * @since 3.1.7
 * @deprecated AIO is explicitly outside the supported NIO monitoring scope.
 */
@Deprecated
public class SniffyAsynchronousChannelProvider extends AsynchronousChannelProvider {

    private static final Polyglog LOG = PolyglogFactory.log(SniffyAsynchronousChannelProvider.class);
    private static boolean unsupportedLogged;

    private final AsynchronousChannelProvider delegate;

    public SniffyAsynchronousChannelProvider(AsynchronousChannelProvider delegate) {
        this.delegate = delegate;
    }

    public static void install() {
        synchronized (SniffyAsynchronousChannelProvider.class) {
            if (!unsupportedLogged) {
                LOG.info("Sniffy does not support NIO2/AIO monitoring; the asynchronous provider was not changed");
                unsupportedLogged = true;
            }
        }
    }

    public static void uninstall() {
        // No-op: install() deliberately leaves the global AIO provider unchanged.
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads, ThreadFactory threadFactory) throws IOException {
        return delegate.openAsynchronousChannelGroup(nThreads, threadFactory);
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor, int initialSize) throws IOException {
        return delegate.openAsynchronousChannelGroup(executor, initialSize);
    }

    @Override
    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return delegate.openAsynchronousServerSocketChannel(group);
    }

    @Override
    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return delegate.openAsynchronousSocketChannel(group);
    }
}
