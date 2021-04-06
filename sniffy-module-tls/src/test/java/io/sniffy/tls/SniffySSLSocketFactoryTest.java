package io.sniffy.tls;

import io.sniffy.socket.BaseSocketTest;
import org.junit.Test;
import sun.security.jca.Providers;

import javax.net.ssl.SSLSocketFactory;
import java.net.Socket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SniffySSLSocketFactoryTest extends BaseSocketTest {

    @Test
    public void testCreateSocket() throws Exception {

        try {
            SniffyTlsModule.initialize();

            Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());

            assertTrue(socket instanceof SniffySSLSocket);

        } finally {
            SniffyTlsModule.uninstall();
        }

    }

}