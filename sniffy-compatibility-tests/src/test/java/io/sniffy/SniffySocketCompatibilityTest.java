package io.sniffy;

import io.sniffy.socket.SnifferSocketImplFactory;
import org.junit.Test;

public class SniffySocketCompatibilityTest {

    @Test
    public void testInstallUninstall() throws Exception {
        SnifferSocketImplFactory.install();
        SnifferSocketImplFactory.uninstall();
    }

}
