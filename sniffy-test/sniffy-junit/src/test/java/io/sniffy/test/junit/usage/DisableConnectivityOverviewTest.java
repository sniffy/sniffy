package io.sniffy.test.junit.usage;

import io.sniffy.socket.DisableSockets;
import io.sniffy.test.junit.SniffyRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class DisableConnectivityOverviewTest {
    // tag::DisableSocketsOverview[]
    @Rule public SniffyRule sniffyRule = new SniffyRule();

    @Test
    @DisableSockets
    public void testDisableSockets() throws IOException {
        try {
            new Socket("google.com", 22);
            fail("Sniffy should have thrown ConnectException");
        } catch (ConnectException e) {
            assertNotNull(e);
        }
    }
    // end::DisableSocketsOverview[]
}
