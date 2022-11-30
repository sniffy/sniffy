package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.PolyglogLevel;
import io.sniffy.reflection.Unsafe;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.util.OSUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DecryptBouncyCastleGoogleTrafficTest {

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @Test
    public void testGoogleTraffic() throws Exception {

        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);

        assertEquals("BCJSSE", SSLContext.getInstance("Default").getProvider().getName());
        assertNotNull(SSLContext.getInstance("TLSv1", "BCJSSE"));

        SniffyConfiguration.INSTANCE.setLogLevel(PolyglogLevel.TRACE);
        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setPacketMergeThreshold(10000);
        Sniffy.initialize();

        SSLContext instance = null;
        try {
            instance = SSLContext.getInstance("TLSv1", "BCJSSE");
        } catch (NoSuchProviderException e) {
            assert false : "Couldn't find provider BCJSSE; actual list was " + Arrays.toString(Security.getProviders());
        }
        instance.init(null, null, new SecureRandom());
        assertTrue(instance.getSocketFactory() instanceof SniffySSLSocketFactory);

        Provider sniffyProvider = SSLContext.getInstance("Default").getProvider();
        assertEquals("Sniffy-BCJSSE", sniffyProvider.getName());

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            for (int i = 0; i < 10; i++) {

                try {
                    URL url = new URL("https://www.google.com");
                    URLConnection urlConnection = url.openConnection();

                    urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

                    // On Java 14 with parallel builds sometimes throws SSLException: An established connection was aborted by the software in your host machine
                    //noinspection ResultOfMethodCallIgnored
                    urlConnection.getInputStream().read();

                    break;
                } catch (IOException e) {
                    if ((
                            e.getMessage().contains("An established connection was aborted by the software in your host machine") ||
                                    e.getMessage().contains("handshake_failure(40)")
                    ) && OSUtil.isWindows() && (Unsafe.tryGetJavaVersion() == 14 || Unsafe.tryGetJavaVersion() == 13)) {
                        e.printStackTrace();
                        System.err.println("Caught " + e + " exception on Java " + Unsafe.tryGetJavaVersion() + " running on Windows; retrying in 2 seconds");
                        Thread.sleep(2000);
                    } else if (e.getMessage().contains("Broken pipe") && OSUtil.isMac() && (Unsafe.tryGetJavaVersion() >= 13)) {
                        e.printStackTrace();
                        System.err.println("Caught " + e + " exception on Java " + Unsafe.tryGetJavaVersion() + " running on Mac OS; retrying in 2 seconds");
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

            //noinspection SimplifiableAssertion
            assertEquals(true, request.isSent());
            //noinspection SimplifiableAssertion
            assertEquals(false, response.isSent());

            assertTrue(new String(request.getBytes(), Charset.forName("US-ASCII")).contains("Host: www.google.com"));
            assertTrue(new String(response.getBytes(), Charset.forName("US-ASCII")).contains("200"));

        }

    }

}
