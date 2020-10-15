package io.sniffy;

import io.sniffy.nio.SniffySelectorProvider;
import io.sniffy.nio.SniffySelectorProviderBootstrap;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.SnifferSocketImplFactory;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SniffyChannelCompatibilityTest {

    protected final static byte[] RESPONSE = new byte[]{9, 8, 7, 6, 5, 4, 3, 2};
    protected final static byte[] REQUEST = new byte[]{1, 2, 3, 4};

    protected static InetAddress localhost;

    @Rule
    public EchoServerRule echoServerRule = new EchoServerRule(RESPONSE);

    @BeforeClass
    public static void resolveLocalhost() throws UnknownHostException {
        localhost = InetAddress.getByName(null);
    }

    @Test
    public void testBlockSocketChannel() throws Exception {

        try {
            SniffySelectorProviderBootstrap.initialize();
            SniffySelectorProvider.install();
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(localhost.getHostName(), echoServerRule.getBoundPort(), -1);

            SocketChannel client = SocketChannel.open(new InetSocketAddress(localhost, echoServerRule.getBoundPort()));
            client.write(ByteBuffer.wrap(REQUEST));

            fail("Should have been blocked by Sniffy");
        } catch (ConnectException e) {
            assertTrue(e.getMessage().contains("refused by Sniffy"));
        } finally {
            ConnectionsRegistry.INSTANCE.clear();
            SnifferSocketImplFactory.uninstall();
        }

    }

}
