package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.spi.SelectorProvider;
import java.lang.reflect.Field;
import java.net.StandardProtocolFamily;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static org.junit.Assume.assumeTrue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SniffySelectorProviderInstallationTest {

    @Before
    public void openJdkNioAccessAndRestoreProvider() {
        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.resetAccessResolverForTests();
    }

    @After
    public void restoreProvider() {
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.resetAccessResolverForTests();
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

    @Test
    public void unsupportedAccessFailsOpenAndCanBeRetried() {
        final SelectorProvider original = SelectorProvider.provider();
        SniffySelectorProvider.setAccessResolverForTests(new NioProviderAccessResolver() {
            @Override
            public NioProviderAccess resolve() throws JdkNioAccess.JdkNioAccessException {
                throw new JdkNioAccess.JdkNioAccessException("simulated unsupported runtime",
                        new UnsupportedOperationException("missing capability"));
            }
        });

        NioInstallationResult unsupported = SniffySelectorProvider.installWithResult();
        assertSame(NioInstallationResult.Status.UNSUPPORTED, unsupported.getStatus());
        assertFalse(unsupported.isInstalled());
        assertSame(original, SelectorProvider.provider());

        SniffySelectorProvider.resetAccessResolverForTests();
        assertTrue(SniffySelectorProvider.installWithResult().isInstalled());
    }

    @Test
    public void partialProviderSwapIsRolledBack() {
        final SelectorProvider original = SelectorProvider.provider();
        final TestProviderAccess access = new TestProviderAccess(original, true);
        SniffySelectorProvider.setAccessResolverForTests(new FixedAccessResolver(access));

        NioInstallationResult failed = SniffySelectorProvider.installWithResult();

        assertSame(NioInstallationResult.Status.FAILED, failed.getStatus());
        assertFalse(failed.isInstalled());
        assertSame(original, access.provider);
        assertSame(original, SelectorProvider.provider());
    }

    @Test
    public void detectsProviderThatIsAlreadyWrapped() {
        SelectorProvider original = SelectorProvider.provider();
        TestProviderAccess access = new TestProviderAccess(new SniffySelectorProvider(original), false);
        SniffySelectorProvider.setAccessResolverForTests(new FixedAccessResolver(access));

        NioInstallationResult result = SniffySelectorProvider.installWithResult();

        assertSame(NioInstallationResult.Status.ALREADY_INSTALLED, result.getStatus());
        assertTrue(result.isInstalled());
    }

    @Test
    public void sniffyModuleFlagTracksFailureAndReinitializeRetries() throws Exception {
        final SelectorProvider original = SelectorProvider.provider();
        final Field loadedField = Sniffy.class.getDeclaredField("nioModuleLoaded");
        loadedField.setAccessible(true);
        boolean previousMonitorNio = SniffyConfiguration.INSTANCE.isMonitorNio();
        loadedField.setBoolean(null, false);
        SniffySelectorProvider.setAccessResolverForTests(new NioProviderAccessResolver() {
            @Override
            public NioProviderAccess resolve() throws JdkNioAccess.JdkNioAccessException {
                throw new JdkNioAccess.JdkNioAccessException("simulated module failure",
                        new UnsupportedOperationException("missing capability"));
            }
        });
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        try {
            Sniffy.reinitialize();
            assertFalse(loadedField.getBoolean(null));
            assertSame(original, SelectorProvider.provider());

            SniffySelectorProvider.resetAccessResolverForTests();
            Sniffy.reinitialize();
            assertTrue(loadedField.getBoolean(null));
            assertTrue(SelectorProvider.provider() instanceof SniffySelectorProvider);
        } finally {
            SniffySelectorProvider.uninstall();
            loadedField.setBoolean(null, false);
            SniffyConfiguration.INSTANCE.setMonitorNio(previousMonitorNio);
        }
    }

    @Test
    public void protocolFamilyOverloadsReturnMonitoredTcpChannels() throws Exception {
        assumeTrue(runtimeFeatureVersion() >= 15);
        SniffySelectorProvider.install();
        SniffySelectorProvider provider = (SniffySelectorProvider) SelectorProvider.provider();

        try (SocketChannel socket = provider.openSocketChannel(StandardProtocolFamily.INET);
             ServerSocketChannel server = provider.openServerSocketChannel(StandardProtocolFamily.INET)) {
            assertTrue(socket instanceof SniffySocketChannel);
            assertTrue(server instanceof SniffyServerSocketChannel);
        }
    }

    private static class FixedAccessResolver implements NioProviderAccessResolver {
        private final NioProviderAccess access;

        private FixedAccessResolver(NioProviderAccess access) {
            this.access = access;
        }

        @Override
        public NioProviderAccess resolve() {
            return access;
        }
    }

    private static class TestProviderAccess implements NioProviderAccess {
        private SelectorProvider provider;
        private final boolean rejectWrapper;

        private TestProviderAccess(SelectorProvider provider, boolean rejectWrapper) {
            this.provider = provider;
            this.rejectWrapper = rejectWrapper;
        }

        @Override
        public SelectorProvider getSelectorProvider() {
            return provider;
        }

        @Override
        public void setSelectorProvider(SelectorProvider provider) {
            if (!rejectWrapper || !(provider instanceof SniffySelectorProvider)) {
                this.provider = provider;
            }
        }

        @Override
        public String describeProviderSlot() {
            return "test.provider";
        }
    }

    private static int runtimeFeatureVersion() {
        String version = System.getProperty("java.specification.version", "8");
        if (version.startsWith("1.")) version = version.substring(2);
        int dot = version.indexOf('.');
        return Integer.parseInt(dot < 0 ? version : version.substring(0, dot));
    }
}
