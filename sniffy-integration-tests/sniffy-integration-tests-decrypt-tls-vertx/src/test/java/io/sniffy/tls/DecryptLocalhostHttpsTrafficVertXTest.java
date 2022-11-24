package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.log.PolyglogLevel;
import io.sniffy.reflection.Unsafe;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.PortFactory;
import org.mockserver.socket.tls.KeyStoreFactory;

import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.sniffy.socket.NetworkPacket.convertNetworkPacketsToString;
import static org.junit.Assert.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class DecryptLocalhostHttpsTrafficVertXTest {

    private static final Polyglog LOG = PolyglogFactory.log(DecryptLocalhostHttpsTrafficVertXTest.class);

    @BeforeClass
    public static void loadTlsModule() {
        HttpsURLConnection.setDefaultSSLSocketFactory(new KeyStoreFactory(new MockServerLogger()).sslContext().getSocketFactory());
        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setLogLevel(PolyglogLevel.TRACE);
        SniffyConfiguration.INSTANCE.setPacketMergeThreshold(10000);
        Sniffy.initialize();
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

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @Test
    public void testLocalhostHttpsTraffic() throws Exception {

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            Vertx vertx = Vertx.vertx();
            HttpRequest<Buffer> httpRequest = WebClient.create(vertx)
                    .get(port, "localhost", "/")
                    .ssl(true)
                    .expect(ResponsePredicate.SC_OK);

            CyclicBarrier cb = new CyclicBarrier(2);

            AtomicBoolean success = new AtomicBoolean(false);

            httpRequest.send(asyncResult -> {
                LOG.debug("HTTP Request complete; received result " + asyncResult + "; succeeded=" + asyncResult.succeeded());
                if (asyncResult.failed()) {
                    asyncResult.cause().printStackTrace();
                    System.err.println(asyncResult.result().bodyAsString());
                }
                success.set(asyncResult.succeeded());
                try {
                    cb.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw Unsafe.throwException(e);
                }
            });

            cb.await();

            assertTrue(success.get());

            {
                Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic(
                        Threads.ANY,
                        AddressMatchers.exactAddressMatcher("localhost:" + port),
                        GroupingOptions.builder().
                                groupByConnection(false).
                                groupByStackTrace(false).
                                groupByThread(false).
                                build()
                );

                assertEquals(1, networkTraffic.size());
            }

            Map<SocketMetaData, List<NetworkPacket>> decryptedNetworkTraffic = spy.getDecryptedNetworkTraffic(
                    Threads.ANY,
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

            //noinspection SimplifiableAssertion
            assertEquals(true, request.isSent());
            //noinspection SimplifiableAssertion
            assertEquals(false, response.isSent());

            assertTrue(new String(request.getBytes(), Charset.forName("US-ASCII")).toLowerCase(Locale.ROOT).contains("host: localhost"));
            assertTrue(new String(response.getBytes(), Charset.forName("US-ASCII")).contains("200"));

        }

    }

}
