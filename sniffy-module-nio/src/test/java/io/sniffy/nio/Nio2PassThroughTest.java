package io.sniffy.nio;

import org.junit.Test;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class Nio2PassThroughTest {

    @Test
    public void legacyInstallMethodLeavesAioProviderUnchanged() throws Exception {
        AsynchronousChannelProvider original = AsynchronousChannelProvider.provider();

        SniffyAsynchronousChannelProvider.install();

        assertSame(original, AsynchronousChannelProvider.provider());
        try (AsynchronousSocketChannel channel = AsynchronousSocketChannel.open()) {
            assertFalse(channel instanceof SniffyAsynchronousSocketChannel);
        } finally {
            SniffyAsynchronousChannelProvider.uninstall();
        }
    }
}
