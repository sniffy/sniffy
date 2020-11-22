package io.sniffy;

import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.SnifferSocketImplFactory;
import org.junit.Test;

import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SniffySocketCompatibilityTest {

    @Test
    public void testBlockHttpUrlConnection() throws Exception {
        try {
            SnifferSocketImplFactory.install();
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("google.com", 443, -1);

            URL url = new URL("https://google.com");
            URLConnection urlConnection = url.openConnection();
            urlConnection.getInputStream().read();

            fail("Should have been blocked by Sniffy");
        } catch (ConnectException e) {
            assertTrue(e.getMessage().contains("refused by Sniffy"));
        } finally {
            ConnectionsRegistry.INSTANCE.clear();
            SnifferSocketImplFactory.uninstall();
        }

    }

}
