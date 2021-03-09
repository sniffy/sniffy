package io.sniffy.tls;

import org.junit.Test;
import sun.security.jca.Providers;

import java.security.Provider;

import static org.junit.Assert.*;

public class SniffySSLContextProviderTest {

    @Test
    public void testInstall() {

        try {
            SniffyProviderListUtil.install();

            assertTrue(Providers.getProviderList().providers().get(0) instanceof SniffySSLContextProvider);

        } finally {
            SniffyProviderListUtil.uninstall();
        }

    }

    @Test
    public void testUninstall() {

        try {
            SniffyProviderListUtil.install();
            SniffyProviderListUtil.uninstall();

            assertFalse(Providers.getProviderList().providers().stream().anyMatch(provider -> provider instanceof SniffySSLContextProvider));

        } finally {
            SniffyProviderListUtil.uninstall();
        }

    }

}