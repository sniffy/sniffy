package io.sniffy.tls;

import io.sniffy.socket.BaseSocketTest;
import org.junit.Test;
import sun.security.jca.Providers;

import javax.net.ssl.SSLSocketFactory;
import java.net.Socket;

import static org.junit.Assert.*;

public class SniffySSLContextSpiProviderTest extends BaseSocketTest {

    @Test
    public void testInstall() throws Exception {

        try {
            SniffyTlsModule.initialize();

            assertTrue(Providers.getProviderList().providers().get(0) instanceof SniffySSLContextSpiProvider);

        } finally {
            SniffyTlsModule.uninstall();
        }

    }

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

    @Test
    public void testUninstall() throws Exception {

        try {
            SniffyTlsModule.initialize();

            assertFalse(Providers.getProviderList().providers().stream().anyMatch(provider -> provider instanceof SniffySSLContextSpiProvider));

        } finally {
            SniffyTlsModule.uninstall();
        }

    }

}