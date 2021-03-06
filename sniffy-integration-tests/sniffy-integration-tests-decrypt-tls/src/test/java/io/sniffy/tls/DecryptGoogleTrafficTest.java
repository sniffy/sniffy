package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import org.junit.BeforeClass;
import org.junit.Test;

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
        Sniffy.initialize();
    }

    @Test
    public void testGoogleTraffic() throws Exception {

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            URL url = new URL("https://www.google.com");
            URLConnection urlConnection = url.openConnection();

            urlConnection.getInputStream().read();

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
