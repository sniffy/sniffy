package io.sniffy.socket;

import io.sniffy.Sniffer;
import io.sniffy.Spy;
import org.junit.Test;

import static io.sniffy.Threads.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// TODO: test with another socket factory which is already set
public class SnifferSocketImplFactoryTest extends BaseSocketTest {

    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        try (Spy<?> s = Sniffer.spy()) {

            performSocketOperation();

            Thread thread = new Thread(this::performSocketOperation);
            thread.start();
            thread.join();

            // Current thread socket operations

            assertEquals(1, s.getSocketOperations(CURRENT).entrySet().stream().count());

            s.getSocketOperations(CURRENT).values().stream().findAny().ifPresent((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

            // Other threads socket operations

            assertEquals(1, s.getSocketOperations(OTHERS).entrySet().stream().count());

            s.getSocketOperations(OTHERS).values().stream().findAny().ifPresent((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

            // Any threads socket operations

            assertEquals(2, s.getSocketOperations(ANY).entrySet().stream().count());

            s.getSocketOperations(OTHERS).values().stream().forEach((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

        }

    }

    @Test
    public void testUninstall() throws Exception {

        Sniffer.initialize();

        SnifferSocketImplFactory.uninstall();

        try (Spy<?> s = Sniffer.spy()) {

            performSocketOperation();

            assertTrue(s.getSocketOperations(CURRENT).isEmpty());

        }

        SnifferSocketImplFactory.install();

    }

}