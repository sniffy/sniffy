package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.sniffy.socket.NetworkPacket.convertNetworkPacketsToString;
import static org.junit.Assert.*;

public class DecryptBouncyCastleLocalTrafficTest {

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @Test
    public void testLocalTraffic() throws Exception {

        try (BouncyCastleHttpsServer httpsServer = BouncyCastleHttpsServer.start()) {

            Security.insertProviderAt(new BouncyCastleProvider(), 1);
            Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);

            assertEquals("BCJSSE", SSLContext.getInstance("Default").getProvider().getName());

            SniffyConfiguration.INSTANCE.setDecryptTls(true);
            SniffyConfiguration.INSTANCE.setMonitorSocket(true);
            SniffyConfiguration.INSTANCE.setPacketMergeThreshold(10000);
            Sniffy.initialize();

            SSLContext instance = SSLContext.getInstance("TLSv1.2", "BCJSSE");
            instance.init(null, httpsServer.createTrustManagers(), new SecureRandom());
            assertTrue(instance.getSocketFactory() instanceof SniffySSLSocketFactory);

            Provider sniffyProvider = SSLContext.getInstance("Default").getProvider();
            assertEquals("Sniffy-BCJSSE", sniffyProvider.getName());

            try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

                URL url = new URL("https://" + BouncyCastleHttpsServer.HOST + ":" + httpsServer.getPort() +
                        BouncyCastleHttpsServer.PATH);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setSSLSocketFactory(instance.getSocketFactory());
                urlConnection.setRequestProperty("User-Agent", "Sniffy-BCJSSE-Test");

                String responseBody;
                try {
                    assertEquals(200, urlConnection.getResponseCode());
                    responseBody = read(urlConnection.getInputStream());
                } finally {
                    urlConnection.disconnect();
                }
                assertEquals(BouncyCastleHttpsServer.RESPONSE_BODY, responseBody);

                String serverRequest = httpsServer.awaitRequest();
                assertTrue(serverRequest.startsWith("GET " + BouncyCastleHttpsServer.PATH + " HTTP/1.1\r\n"));
                assertTrue(serverRequest.toLowerCase(Locale.ROOT).contains(
                        "host: " + httpsServer.getAddress().toLowerCase(Locale.ROOT)));

                Map<SocketMetaData, List<NetworkPacket>> decryptedNetworkTraffic = spy.getDecryptedNetworkTraffic(
                        Threads.CURRENT,
                        AddressMatchers.exactAddressMatcher(httpsServer.getAddress()),
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

                assertEquals("Expected one decrypted request and one decrypted response, but got " +
                                convertNetworkPacketsToString(entry.getValue()),
                        2, entry.getValue().size());

                NetworkPacket request = entry.getValue().get(0);
                NetworkPacket response = entry.getValue().get(1);

                assertTrue(request.isSent());
                assertFalse(response.isSent());

                String decryptedRequest = new String(request.getBytes(), Charset.forName("US-ASCII"));
                String decryptedResponse = new String(response.getBytes(), Charset.forName("US-ASCII"));
                assertTrue(decryptedRequest.startsWith("GET " + BouncyCastleHttpsServer.PATH + " HTTP/1.1\r\n"));
                assertTrue(decryptedRequest.toLowerCase(Locale.ROOT).contains(
                        "host: " + httpsServer.getAddress().toLowerCase(Locale.ROOT)));
                assertTrue(decryptedResponse.startsWith("HTTP/1.1 200 OK\r\n"));
                assertTrue(decryptedResponse.contains(BouncyCastleHttpsServer.RESPONSE_BODY));

            }
        }
    }

    private static String read(InputStream inputStream) throws Exception {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            return new String(outputStream.toByteArray(), Charset.forName("US-ASCII"));
        } finally {
            inputStream.close();
        }
    }
}
