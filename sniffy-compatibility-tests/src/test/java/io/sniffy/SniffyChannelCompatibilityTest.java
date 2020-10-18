package io.sniffy;

import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.SnifferSocketImplFactory;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

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
            SniffyConfiguration.INSTANCE.setMonitorNio(true);
            Sniffy.initialize();
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

    @Test
    public void testPipe() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        Sniffy.initialize();

        Pipe pipe = Pipe.open();

        final Pipe.SourceChannel source = pipe.source();
        final Pipe.SinkChannel sink = pipe.sink();

        final ByteBuffer targetBuffer = ByteBuffer.allocate(5);
        final AtomicReference<Exception> exceptionHolder = new AtomicReference<Exception>();

        Thread sourceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    source.read(targetBuffer);
                } catch (IOException e) {
                    exceptionHolder.set(e);
                }
            }
        });

        Thread sinkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sink.write(ByteBuffer.wrap(new byte[]{1, 2, 3, 5, 8}));
                } catch (IOException e) {
                    exceptionHolder.set(e);
                }
            }
        });

        sourceThread.start();
        sinkThread.start();
        sourceThread.join();
        sinkThread.join();

        assertArrayEquals(new byte[]{1, 2, 3, 5, 8}, targetBuffer.array());

    }

}
