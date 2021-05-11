package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.Charset;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class DecryptGoogleTrafficTest {

    @BeforeClass
    public static void loadTlsModuleAndSetupBouncyCastle() {

        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);

        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        Sniffy.initialize();

    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @Test
    public void testGoogleTraffic() throws Exception {

        Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build());

        CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
        httpclient.start();
        HttpGet request = new HttpGet("https://www.google.com");
        Future<HttpResponse> future = httpclient.execute(request, null);
        HttpResponse response = future.get();
        assertEquals(200, response.getStatusLine().getStatusCode());
        httpclient.close();

        Map<SocketMetaData, List<NetworkPacket>> decryptedNetworkTraffic = spy.getDecryptedNetworkTraffic(
                Threads.ANY,
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

        NetworkPacket requestPacket = entry.getValue().get(0);
        NetworkPacket responsePacket = entry.getValue().get(1);

        //noinspection SimplifiableAssertion
        assertEquals(true, requestPacket.isSent());
        //noinspection SimplifiableAssertion
        assertEquals(false, responsePacket.isSent());

        assertTrue(new String(requestPacket.getBytes(), Charset.forName("US-ASCII")).contains("Host: www.google.com"));
        assertTrue(new String(responsePacket.getBytes(), Charset.forName("US-ASCII")).contains("200"));

    }

}
