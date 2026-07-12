package io.sniffy.nio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.spi.SelectorProvider;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SniffySelectorProviderInstallationTest {

    @Before
    public void openJdkNioAccessAndRestoreProvider() {
        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
    }

    @After
    public void restoreProvider() {
        SniffySelectorProvider.uninstall();
    }

    @Test
    public void installationIsIdempotentAndReportsItsState() {
        NioInstallationResult installed = SniffySelectorProvider.installWithResult();
        SelectorProvider provider = SelectorProvider.provider();
        NioInstallationResult alreadyInstalled = SniffySelectorProvider.installWithResult();

        assertSame(NioInstallationResult.Status.INSTALLED, installed.getStatus());
        assertTrue(installed.isInstalled());
        assertTrue(provider instanceof SniffySelectorProvider);
        assertSame(NioInstallationResult.Status.ALREADY_INSTALLED, alreadyInstalled.getStatus());
        assertSame(provider, SelectorProvider.provider());
    }

    @Test
    public void providerCanBeUninstalledAndReinstalled() {
        SelectorProvider original = SelectorProvider.provider();

        assertTrue(SniffySelectorProvider.installWithResult().isInstalled());
        NioInstallationResult uninstalled = SniffySelectorProvider.uninstallWithResult();

        assertSame(NioInstallationResult.Status.UNINSTALLED, uninstalled.getStatus());
        assertSame(original, SelectorProvider.provider());
        assertTrue(SniffySelectorProvider.installWithResult().isInstalled());
        assertTrue(SelectorProvider.provider() instanceof SniffySelectorProvider);
    }
}
