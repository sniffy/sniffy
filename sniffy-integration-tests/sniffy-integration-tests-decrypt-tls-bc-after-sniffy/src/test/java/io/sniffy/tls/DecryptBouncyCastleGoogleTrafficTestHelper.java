package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.OSUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.*;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DecryptBouncyCastleGoogleTrafficTestHelper {

    public static void testGoogleTrafficImpl() throws Exception {
        assertTrue(SSLContext.getInstance("Default").getProvider().getName().contains("Sniffy"));

        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);

        // Sniffy.reinitialize(); // https://github.com/sniffy/sniffy/issues/478 - bug in io.sniffy.tls.SniffyProviderListUtil

        SSLContext instance = SSLContext.getInstance("TLSv1.2", "BCJSSE");
        instance.init(null, null, new SecureRandom());
        assertTrue(instance.getSocketFactory() instanceof SniffySSLSocketFactory);

        assertEquals("Sniffy-BCJSSE", SSLContext.getDefault().getProvider().getName());

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
                    ) && OSUtil.isWindows() && (JVMUtil.getVersion() == 14 || JVMUtil.getVersion() == 13)) {
                        e.printStackTrace();
                        System.err.println("Caught " + e + " exception on Java " + JVMUtil.getVersion() + " running on Windows; retrying in 2 seconds");
                        Thread.sleep(2000);
                    } else if (e.getMessage().contains("Broken pipe") && OSUtil.isMac() && (JVMUtil.getVersion() >= 13)) {
                        e.printStackTrace();
                        System.err.println("Caught " + e + " exception on Java " + JVMUtil.getVersion() + " running on Mac OS; retrying in 2 seconds");
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

            //noinspection CharsetObjectCanBeUsed
            assertTrue(new String(request.getBytes(), Charset.forName("US-ASCII")).contains("Host: www.google.com"));
            //noinspection CharsetObjectCanBeUsed
            assertTrue(new String(response.getBytes(), Charset.forName("US-ASCII")).contains("200"));

        }
    }

}
