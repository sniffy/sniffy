package io.sniffy.tls;

import io.sniffy.socket.BaseSocketTest;
import org.junit.Test;
import sun.security.jca.Providers;

import javax.net.ssl.SSLSocketFactory;
import java.net.Socket;
import java.security.Provider;
import java.security.Security;

import static org.junit.Assert.*;

public class SniffySSLContextProviderTest extends BaseSocketTest {

    @Test
    public void testInstall() throws Exception {

        try {
            SniffyTlsModule.initialize();
            SniffyProviderListUtil.install();

            assertTrue(Providers.getProviderList().providers().get(0) instanceof SniffySSLContextProvider);

        } finally {
            SniffyProviderListUtil.uninstall();
        }

    }

    @Test
    public void testCreateSocket() throws Exception {

        try {
            SniffyTlsModule.initialize();
            SniffyProviderListUtil.install();

            Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());

            assertTrue(socket instanceof SniffySSLSocket);

        } finally {
            SniffyProviderListUtil.uninstall();
        }

    }

    @Test
    public void testUninstall() throws Exception {

        try {
            SniffyTlsModule.initialize();
            SniffyProviderListUtil.install();
            SniffyProviderListUtil.uninstall();

            assertFalse(Providers.getProviderList().providers().stream().anyMatch(provider -> provider instanceof SniffySSLContextProvider));

        } finally {
            SniffyProviderListUtil.uninstall();
        }

    }

}