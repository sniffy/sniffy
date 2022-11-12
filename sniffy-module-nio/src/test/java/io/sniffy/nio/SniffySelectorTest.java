package io.sniffy.nio;

import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.util.ObjectWrapper;
import org.junit.Test;

import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SniffySelectorTest {

    @Test
    public void testCloseSelector() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();

        try (Selector selector = Selector.open()) {
            selector.close();
            assertFalse(selector.isOpen());
            assertTrue(selector instanceof ObjectWrapper);
            //noinspection unchecked
            AbstractSelector delegate = ((ObjectWrapper<AbstractSelector>) selector).getDelegate();
            assertFalse(delegate.isOpen());
        }

    }

}
