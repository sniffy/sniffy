package io.sniffy.tls;

import org.junit.Test;
import sun.security.jca.Providers;

import java.security.Provider;

import static org.junit.Assert.*;

public class SniffySSLContextProviderTest {

    @Test
    public void testInstall() {

        try {
            SniffySSLContextProvider.install();

            assertTrue(Providers.getProviderList().providers().get(0) instanceof SniffySSLContextProvider);

        } finally {
            SniffySSLContextProvider.uninstall();
        }

    }

    @Test
    public void testUninstall() {

        try {
            SniffySSLContextProvider.install();
            SniffySSLContextProvider.uninstall();

            assertFalse(Providers.getProviderList().providers().stream().anyMatch(provider -> provider instanceof SniffySSLContextProvider));

        } finally {
            SniffySSLContextProvider.uninstall();
        }

    }

}