package io.sniffy.socket;

import org.junit.After;
import org.junit.Test;

import java.net.ConnectException;
import java.net.Socket;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SocketRegistryTest extends BaseSocketTest {

    @After
    public void clearConnectionRules() {
        SocketsRegistry.INSTANCE.clear();
    }

    @Test
    public void testConnectionClosed() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SocketsRegistry.INSTANCE.setSocketAddressStatus(localhost.getHostName(), echoServerRule.getBoundPort(), SocketsRegistry.SocketAddressStatus.CLOSED);

        Socket socket = null;

        try {
            socket = new Socket(localhost, echoServerRule.getBoundPort());
            fail("Should hve failed since this connection is forbidden by sniffy");
        } catch (ConnectException e) {
            assertNotNull(e);
        } finally {
            if (null != socket) socket.close();
        }

    }

    @Test
    public void testConnectionOpened() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        new Socket(localhost, echoServerRule.getBoundPort()).close();

    }

}