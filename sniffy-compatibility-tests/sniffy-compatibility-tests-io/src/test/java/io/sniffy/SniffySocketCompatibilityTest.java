package io.sniffy;

import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.OSUtil;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.io.IOException;
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
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("www.google.com", 443, -1);

            for (int i = 0; i < 10; i++) {

                try {
                    URL url = new URL("https://www.google.com");
                    URLConnection urlConnection = url.openConnection();

                    // On Java 14 with parallel builds sometimes throws SSLException: An established connection was aborted by the software in your host machine
                    //noinspection ResultOfMethodCallIgnored
                    urlConnection.getInputStream().read();

                    break;
                } catch (IOException e) {
                    if (e.getMessage().contains("An established connection was aborted by the software in your host machine") && OSUtil.isWindows() && (JVMUtil.getVersion() == 14 || JVMUtil.getVersion() == 13)) {
                        e.printStackTrace();
                        System.err.println("Caught " + e + " exception on Java " + JVMUtil.getVersion() + " running on Windows; retrying in 2 seconds");
                        Thread.sleep(2000);
                    } else {
                        throw e;
                    }
                }

            }

            fail("Should have been blocked by Sniffy");
        } catch (ConnectException e) {
            assertTrue(e.getMessage().contains("refused by Sniffy"));
        } finally {
            ConnectionsRegistry.INSTANCE.clear();
            SnifferSocketImplFactory.uninstall();
        }

    }

}
