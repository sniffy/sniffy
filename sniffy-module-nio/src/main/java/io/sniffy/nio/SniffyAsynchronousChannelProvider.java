package io.sniffy.nio;

import io.sniffy.util.ReflectionUtil;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

// TODO: this functionality is available in java 1.7+ only - make sure it is safe
// TODO: integrate with Sniffy; currently it is not used
/**
 * @since 3.1.7
 */
public class SniffyAsynchronousChannelProvider extends AsynchronousChannelProvider {

    private final AsynchronousChannelProvider delegate;

    public SniffyAsynchronousChannelProvider(AsynchronousChannelProvider delegate) {
        this.delegate = delegate;
    }

    public static void install() {
        AsynchronousChannelProvider delegate = AsynchronousChannelProvider.provider();

        if (null != delegate && SniffyAsynchronousChannelProvider.class.equals(delegate.getClass())) {
            return;
        }

        try {
            Class<?> holderClass = Class.forName("java.nio.channels.spi.AsynchronousChannelProvider$ProviderHolder");

            boolean installed = ReflectionUtil.setStaticFinal(
                    holderClass,
                    "provider",
                    new SniffyAsynchronousChannelProvider(delegate)
            );

            if (!installed) {
                new IllegalStateException("Failed to install SniffyAsynchronousChannelProvider").printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void uninstall() {

        // TODO: save default to static field and restore here

        /*try {
            Class<?> holderClass = Class.forName("java.nio.channels.spi.AsynchronousChannelProvider$ProviderHolder");

            Field instanceField = holderClass.getDeclaredField("provider");
            instanceField.setAccessible(true);

            Field modifiersField = getModifiersField();
            modifiersField.setAccessible(true);
            modifiersField.setInt(instanceField, instanceField.getModifiers() & ~Modifier.FINAL);

            instanceField.set(null, delegate);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }*/
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
        return new SniffyAsynchronousSocketChannel(this, delegate.openAsynchronousSocketChannel(group));
    }
}
