package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.PolyglogLevel;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.util.ExceptionUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class DecryptGoogleTrafficTest {

    @BeforeClass
    public static void loadTlsModule() {
        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setLogLevel(PolyglogLevel.TRACE);
        Sniffy.initialize();
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @Test
    public void testGoogleTraffic() throws Exception {

        // TODO: do not use external servers like www.google.com in tests

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            Vertx vertx = Vertx.vertx();
            HttpRequest<Buffer> httpRequest = WebClient.create(vertx)
                    .get(443, "www.google.com", "/")
                    .ssl(true)
                    .expect(ResponsePredicate.SC_OK);

            CyclicBarrier cb = new CyclicBarrier(2);

            AtomicBoolean success = new AtomicBoolean(false);

            httpRequest.send(asyncResult -> {
                success.set(asyncResult.succeeded());
                try {
                    cb.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw ExceptionUtil.throwException(e);
                }
            });

            cb.await();

            assertTrue(success.get());

            {
                Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic(
                        Threads.ANY,
                        AddressMatchers.exactAddressMatcher("www.google.com:443"),
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

            //noinspection SimplifiableAssertion
            assertEquals(true, request.isSent());
            //noinspection SimplifiableAssertion
            assertEquals(false, response.isSent());

            assertTrue(new String(request.getBytes(), Charset.forName("US-ASCII")).toLowerCase(Locale.ROOT).contains("host: www.google.com"));
            assertTrue(new String(response.getBytes(), Charset.forName("US-ASCII")).contains("200"));

        }

    }

}
