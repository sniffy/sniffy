package io.sniffy.tls;

import io.sniffy.socket.BaseSocketTest;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.Test;
import sun.security.jca.Providers;

import javax.net.ssl.SSLSocketFactory;
import java.net.Socket;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;

public class SniffySSLContextSpiProviderTest extends BaseSocketTest {

    @Test
    public void testInstall() throws Exception {

        try {
            SniffyTlsModule.initialize();

            Optional<Provider> sniffyOptionalProvider = Arrays.stream(Security.getProviders()).
                    filter(provider -> null != provider.getService("SSLContext", "Default")).
                    findFirst();

            assertTrue(sniffyOptionalProvider.isPresent());
            assertTrue(sniffyOptionalProvider.get() instanceof SniffySSLContextSpiProvider);

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
            SniffyTlsModule.uninstall();

            assertFalse(Providers.getProviderList().providers().stream().anyMatch(provider -> provider instanceof SniffySSLContextSpiProvider));

        } finally {
            SniffyTlsModule.uninstall();
        }

    }

    @Test
    public void testAliases() throws Exception {

        BouncyCastleJsseProvider delegate = new BouncyCastleJsseProvider();

        SniffySSLContextSpiProvider sniffyProvider = new SniffySSLContextSpiProvider(delegate);

        assertEquals(delegate.getServices().size(), sniffyProvider.getServices().size());

        Provider.Service delegateTrustManagerFactoryService = delegate.getService("TrustManagerFactory", "X.509");
        assertNotNull(delegateTrustManagerFactoryService);

        Provider.Service sniffyTrustManagerFactoryService = sniffyProvider.getService("TrustManagerFactory", "X.509");
        assertNotNull(sniffyTrustManagerFactoryService);

    }

}