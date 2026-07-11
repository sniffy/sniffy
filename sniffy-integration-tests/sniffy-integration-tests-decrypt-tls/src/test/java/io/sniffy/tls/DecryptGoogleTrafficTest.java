package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.sniffy.socket.NetworkPacket.convertNetworkPacketsToString;
import static org.junit.Assert.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class DecryptGoogleTrafficTest {

    @BeforeClass
    public static void loadTlsModule() throws Exception {
        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setPacketMergeThreshold(10000);
        Sniffy.initialize();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManagerFactory trustManagerFactory = createTrustManagerFactory("localhost.crt");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }

    private static TrustManagerFactory createTrustManagerFactory(String certificateResource) throws Exception {
        try (InputStream certInputStream = DecryptGoogleTrafficTest.class.getResourceAsStream("/" + certificateResource)) {
            if (certInputStream == null) {
                throw new IllegalStateException("Certificate resource not found: " + certificateResource);
            }
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(certInputStream);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("mock-server", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            return trustManagerFactory;
        }
    }

    private ClientAndServer mockServer;

    private int port = PortFactory.findFreePort();

    @Before
    public void startMockServer() {
        mockServer = startClientAndServer(port);
        mockServer.when(
                        request()
                                .withMethod("GET")
                                .withPath("/")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                );
    }

    @After
    public void stopMockServer() {
        mockServer.stop();
    }



    @Test
    public void testGoogleTraffic() throws Exception {

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            URL url = new URL("https://localhost:" + port);
            URLConnection urlConnection = url.openConnection();

            //noinspection ResultOfMethodCallIgnored
            urlConnection.getInputStream().read();

            Map<SocketMetaData, List<NetworkPacket>> decryptedNetworkTraffic = spy.getDecryptedNetworkTraffic(
                    Threads.CURRENT,
                    AddressMatchers.exactAddressMatcher("localhost:" + port),
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

            assertEquals("Expected 2 packets, but instead got " + convertNetworkPacketsToString(entry.getValue()), 2, entry.getValue().size());

            NetworkPacket request = entry.getValue().get(0);
            NetworkPacket response = entry.getValue().get(1);

            assertEquals(true, request.isSent());
            assertEquals(false, response.isSent());

            assertTrue(new String(request.getBytes(), Charset.forName("US-ASCII")).toLowerCase(Locale.ROOT).contains("host: localhost"));
            assertTrue(new String(response.getBytes(), Charset.forName("US-ASCII")).contains("200"));

        }

    }

}
