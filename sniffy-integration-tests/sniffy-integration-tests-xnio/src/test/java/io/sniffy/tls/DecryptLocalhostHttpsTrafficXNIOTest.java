package io.sniffy.tls;

import io.qameta.allure.Issue;
import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.log.PolyglogLevel;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.PortFactory;
import org.mockserver.socket.tls.KeyStoreFactory;
import org.xnio.*;
import org.xnio.channels.Channels;
import org.xnio.channels.ConnectedStreamChannel;

import javax.net.ssl.HttpsURLConnection;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.sniffy.socket.NetworkPacket.convertNetworkPacketsToString;
import static org.junit.Assert.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class DecryptLocalhostHttpsTrafficXNIOTest {

    static {
        ScheduledThreadDump.scheduleThreadDump(10); // last mile resort for troubleshooting
    }

    private static final Polyglog LOG = PolyglogFactory.log(DecryptLocalhostHttpsTrafficXNIOTest.class);

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
    @Issue("issues/539")
    // TODO: run this test in repeat mode; it was reproducing https://github.com/sniffy/sniffy/issues/539 only 1 in 50 runs
    public void testLocalhostHttpsTraffic() throws Exception {

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {
        //try {
            final Charset charset = Charset.forName("utf-8");
            final Xnio xnio = Xnio.getInstance();
            final XnioWorker worker = xnio.createWorker(OptionMap.EMPTY);

            try {
                final IoFuture<ConnectedStreamChannel> futureConnection = worker.connectStream(new InetSocketAddress("localhost", port), null, OptionMap.EMPTY);
                final ConnectedStreamChannel channel = futureConnection.get(); // throws exceptions
                try {
                    // Send the greeting
                    Channels.writeBlocking(channel, ByteBuffer.wrap("GET / HTTP/1.1\nHost: localhost\nConnection: Close\n\n".getBytes(charset)));
                    // Make sure all data is written
                    Channels.flushBlocking(channel);
                    // And send EOF
                    channel.shutdownWrites();
                    ByteBuffer recvBuf = ByteBuffer.allocate(128);
                    // Now receive and print the whole response
                    while (Channels.readBlocking(channel, recvBuf) != -1) {
                        recvBuf.flip();
                        final CharBuffer chars = charset.decode(recvBuf);
                        recvBuf.clear();
                    }
                } finally {
                    IoUtils.safeClose(channel);
                }
            } finally {
                worker.shutdown();
            }

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

            Map.Entry<SocketMetaData, List<NetworkPacket>> entry = networkTraffic.entrySet().iterator().next();

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

        } catch (Exception e) {
            System.err.flush();
            System.err.println("Caught interresting exception! <<<");
            e.printStackTrace();
            System.err.println("Caught interresting exception! >>>");
            System.err.flush();
            throw e;
        }

    }

}
