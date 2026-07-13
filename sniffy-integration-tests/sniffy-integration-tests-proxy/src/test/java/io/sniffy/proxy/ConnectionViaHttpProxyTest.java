package io.sniffy.proxy;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.PolyglogLevel;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.tls.BouncyCastleHttpsServer;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class ConnectionViaHttpProxyTest {

    @Rule
    public ProxyServerRule rule = new ProxyServerRule();

    @BeforeClass
    public static void loadTlsModule() {
        SniffyConfiguration.INSTANCE.setLogLevel(PolyglogLevel.TRACE);
        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setPacketMergeThreshold(10000);
        Sniffy.initialize();
    }

    @Test
    public void testLocalTraffic() throws Exception {

        try (BouncyCastleHttpsServer server = BouncyCastleHttpsServer.start();
             Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            try (CloseableHttpAsyncClient httpclient = HttpAsyncClientBuilder.
                        create().
                        setSSLContext(createClientSslContext(server)).
                        setProxy(new HttpHost("localhost", rule.getPortNumber())).
                        build()) {
                httpclient.start();
                HttpGet request = new HttpGet("https://" + server.getAddress() + BouncyCastleHttpsServer.PATH);
                Future<HttpResponse> future = httpclient.execute(request, null);
                HttpResponse response = future.get();
                assertEquals(200, response.getStatusLine().getStatusCode());
                assertEquals(BouncyCastleHttpsServer.RESPONSE_BODY,
                        EntityUtils.toString(response.getEntity(), Charset.forName("US-ASCII")));
            }

            assertFixtureRequest(server.awaitRequest(), server);
            assertDecryptedTraffic(spy, server);

            Map<Map.Entry<String, Integer>, Integer> discoveredAddresses = ConnectionsRegistry.INSTANCE.getDiscoveredAddresses();

            assertTrue(discoveredAddresses.containsKey(targetRegistryAddress(server)));
            assertTrue(discoveredAddresses.containsKey(new AbstractMap.SimpleEntry<>("localhost", rule.getPortNumber())));
        }

    }

    @Test
    public void testLocalTrafficBlockingIO() throws Exception {

        try (BouncyCastleHttpsServer server = BouncyCastleHttpsServer.start();
             Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            try (CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLContext(createClientSslContext(server))
                    .build()) {

                HttpHost target = new HttpHost(BouncyCastleHttpsServer.HOST, server.getPort(), "https");
                HttpHost proxy = new HttpHost("localhost", rule.getPortNumber(), "http");

                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy)
                        .build();
                HttpGet request = new HttpGet(BouncyCastleHttpsServer.PATH);
                request.setConfig(config);

                try (CloseableHttpResponse response = httpclient.execute(target, request)) {
                    assertEquals(200, response.getStatusLine().getStatusCode());
                    assertEquals(BouncyCastleHttpsServer.RESPONSE_BODY,
                            EntityUtils.toString(response.getEntity(), Charset.forName("US-ASCII")));
                }
            }

            assertFixtureRequest(server.awaitRequest(), server);
            assertDecryptedTraffic(spy, server);

            Map<Map.Entry<String, Integer>, Integer> discoveredAddresses = ConnectionsRegistry.INSTANCE.getDiscoveredAddresses();

            assertTrue(discoveredAddresses.containsKey(targetRegistryAddress(server)));
            assertTrue(discoveredAddresses.containsKey(new AbstractMap.SimpleEntry<>("localhost", rule.getPortNumber())));
        }

    }

    private static SSLContext createClientSslContext(BouncyCastleHttpsServer server) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, server.createTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private static Map.Entry<String, Integer> targetRegistryAddress(BouncyCastleHttpsServer server) {
        String hostName = new InetSocketAddress(BouncyCastleHttpsServer.HOST, server.getPort()).getHostName();
        return new AbstractMap.SimpleEntry<>(hostName, server.getPort());
    }

    private static void assertFixtureRequest(String fixtureRequest, BouncyCastleHttpsServer server) {
        assertTrue(fixtureRequest.startsWith("GET " + BouncyCastleHttpsServer.PATH + " HTTP/1.1"));
        assertTrue(fixtureRequest.contains("Host: " + server.getAddress()));
    }

    private static void assertDecryptedTraffic(Spy<?> spy, BouncyCastleHttpsServer server) {
        Map<SocketMetaData, List<NetworkPacket>> decryptedNetworkTraffic = spy.getDecryptedNetworkTraffic(
                Threads.ANY,
                AddressMatchers.exactAddressMatcher(server.getAddress()),
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

        assertTrue(request.isSent());
        assertFalse(response.isSent());

        String requestText = new String(request.getBytes(), Charset.forName("US-ASCII"));
        assertTrue(requestText.startsWith("GET " + BouncyCastleHttpsServer.PATH + " HTTP/1.1"));
        assertTrue(requestText.contains("Host: " + server.getAddress()));

        String responseText = new String(response.getBytes(), Charset.forName("US-ASCII"));
        assertTrue(responseText.startsWith("HTTP/1.1 200"));
        assertTrue(responseText.endsWith(BouncyCastleHttpsServer.RESPONSE_BODY));
    }

}
