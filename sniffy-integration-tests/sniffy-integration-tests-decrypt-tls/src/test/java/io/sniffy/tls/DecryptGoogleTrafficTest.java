package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.reflection.Unsafe;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.util.OSUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DecryptGoogleTrafficTest {

    @BeforeClass
    public static void loadTlsModule() {
        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setPacketMergeThreshold(10000);
        Sniffy.initialize();
    }

    @Test
    public void testGoogleTraffic() throws Exception {

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            for (int i = 0; i < 10; i++) {

                try {
                    URL url = new URL("https://www.google.com");
                    URLConnection urlConnection = url.openConnection();

                    // On Java 14 with parallel builds sometimes throws SSLException: An established connection was aborted by the software in your host machine
                    //noinspection ResultOfMethodCallIgnored
                    urlConnection.getInputStream().read();

                    break;
                } catch (IOException e) {
                    if (e.getMessage().contains("An established connection was aborted by the software in your host machine") || e.getMessage().contains("Remote host terminated the handshake")
                            && OSUtil.isWindows() && (Unsafe.getJavaVersion() == 14 || Unsafe.getJavaVersion() == 13)) {
                        e.printStackTrace();
                        System.err.println("Caught " + e + " exception on Java " + Unsafe.getJavaVersion() + " running on Windows; retrying in 2 seconds");
                        Thread.sleep(2000);
                    } else {
                        throw e;
                    }
                }

            }

            Map<SocketMetaData, List<NetworkPacket>> decryptedNetworkTraffic = spy.getDecryptedNetworkTraffic(
                    Threads.CURRENT,
                    AddressMatchers.exactAddressMatcher("www.google.com:443"),
                    GroupingOptions.builder().
                            groupByConnection(false).
                            groupByStackTrace(false).
                            groupByThread(false).
                            build()
            );

            assertEquals(1, decryptedNetworkTraffic.size());

            Map.Entry<SocketMetaData, List<NetworkPacket>> entry = decryptedNetworkTraffic.entrySet().iterator().next();

            assertNotNull(entry);
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());

            assertEquals(2, entry.getValue().size());

            NetworkPacket request = entry.getValue().get(0);
            NetworkPacket response = entry.getValue().get(1);

            assertEquals(true, request.isSent());
            assertEquals(false, response.isSent());

            assertTrue(new String(request.getBytes(), Charset.forName("US-ASCII")).contains("Host: www.google.com"));
            assertTrue(new String(response.getBytes(), Charset.forName("US-ASCII")).contains("200"));

        }

    }

}
