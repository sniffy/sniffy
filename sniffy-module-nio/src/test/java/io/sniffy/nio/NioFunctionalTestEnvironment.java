package io.sniffy.nio;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

/** Owns the JVM-global provider for the complete functional-test fork. */
final class NioFunctionalTestEnvironment {

    private static final SelectorProvider ORIGINAL_PROVIDER;
    private static final SniffySelectorProvider INSTALLED_PROVIDER;
    private static final NioInstallationResult INSTALLATION_RESULT;

    static {
        SelectorProvider original = SelectorProvider.provider();
        if (original instanceof SniffySelectorProvider) {
            throw new AssertionError("Functional NIO fork did not start from the original provider: "
                    + original.getClass().getName());
        }
        ORIGINAL_PROVIDER = original;
        SniffySelectorProviderModule.initialize();
        INSTALLATION_RESULT = SniffySelectorProvider.getLastInstallationResult();
        SelectorProvider installed = SelectorProvider.provider();
        if (!(installed instanceof SniffySelectorProvider)) {
            throw providerChanged("functional fork bootstrap", installed);
        }
        INSTALLED_PROVIDER = (SniffySelectorProvider) installed;
    }

    private NioFunctionalTestEnvironment() {
    }

    static SelectorProvider originalProvider() {
        assertGloballyInstalled("originalProvider fixture request");
        return ORIGINAL_PROVIDER;
    }

    static AbstractSelector openRawSelector() throws IOException {
        assertGloballyInstalled("raw selector fixture request");
        // WindowsSelectorImpl obtains its wakeup pipe through the global provider. Keep that
        // nested construction raw without changing the JVM-global provider slot.
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return ORIGINAL_PROVIDER.openSelector();
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

    static SniffySelectorProvider installedProvider() {
        assertGloballyInstalled("installedProvider request");
        return INSTALLED_PROVIDER;
    }

    static void assertGloballyInstalled() {
        assertGloballyInstalled("functional test");
    }

    static void assertGloballyInstalled(String testContext) {
        SelectorProvider actual = SelectorProvider.provider();
        if (actual != INSTALLED_PROVIDER) {
            throw providerChanged(testContext, actual);
        }
    }

    private static AssertionError providerChanged(String context, SelectorProvider actual) {
        String result = INSTALLATION_RESULT == null ? "not available"
                : INSTALLATION_RESULT.getStatus() + ": " + INSTALLATION_RESULT.getMessage();
        return new AssertionError("Global SelectorProvider changed during " + context
                + "; expected=" + className(INSTALLED_PROVIDER)
                + "; actual=" + className(actual)
                + "; installationResult=" + result
                + "; thread=" + Thread.currentThread().getName());
    }

    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
