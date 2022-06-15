package io.sniffy.test.testng;

import io.qameta.allure.Feature;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.DisableSockets;
import org.testng.annotations.*;

import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Listeners(SniffyTestNgListener.class)
public class DisableSocketsTestNg extends BaseSocketTest {

    @BeforeClass
    public void resolveLocalHost() throws UnknownHostException {
        BaseSocketTest.resolveLocalhost();
    }

    @BeforeMethod
    public void startEchoServer() throws Throwable {
        echoServerRule.before();
    }
    @AfterMethod
    public void stopEchoServer() throws Throwable {
        echoServerRule.after();
    }

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
