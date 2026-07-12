package io.sniffy.nio;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/** Installs once at functional-fork bootstrap and checks ownership before every test. */
public final class NioFunctionalTestRunListener extends RunListener {

    public NioFunctionalTestRunListener() {
        NioFunctionalTestEnvironment.assertGloballyInstalled("functional fork listener bootstrap");
    }

    @Override
    public void testStarted(Description description) {
        NioFunctionalTestEnvironment.assertGloballyInstalled(description.getDisplayName());
    }
}
