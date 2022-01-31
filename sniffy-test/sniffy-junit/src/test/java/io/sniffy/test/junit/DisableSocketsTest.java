package io.sniffy.test.junit;

import io.qameta.allure.Feature;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.DisableSockets;
import org.junit.Rule;
import org.junit.Test;

import java.net.ConnectException;
import java.net.Socket;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DisableSocketsTest extends BaseSocketTest {

    @Rule
    public SniffyRule sniffyRule = new SniffyRule();

    @DisableSockets
    @Test
    @Feature("issues/224")
    public void testAllConnectionsDisabled() {

        try {
            new Socket(localhost, echoServerRule.getBoundPort());
            fail("Connection should have been refused by Sniffy");
        } catch (ConnectException e) {
            assertTrue(e.getMessage().contains("refused by Sniffy"));
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            echoServerRule.joinThreads();
        }

    }

}
