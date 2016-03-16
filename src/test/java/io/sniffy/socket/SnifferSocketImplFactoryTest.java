package io.sniffy.socket;

import io.sniffy.Sniffer;
import io.sniffy.Spy;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SnifferSocketImplFactoryTest {

    @Rule
    public EchoServerRule echoServerRule = new EchoServerRule();

    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.install();

        try (Spy<?> s = Sniffer.spy()) {

            InetAddress localhost = InetAddress.getByName(null);
            Socket socket = new Socket(localhost, echoServerRule.getBoundPort());

            assertTrue(socket.isConnected());
            echoServerRule.getCountDownLatch().countDown();
            echoServerRule.getCountDownLatch().await();

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(new byte[]{1, 2, 3, 4});
            outputStream.flush();
            socket.shutdownOutput();

            InputStream inputStream = socket.getInputStream();
            while (inputStream.read() != -1);

            echoServerRule.joinThreads();

            assertFalse(
                    s.getSocketOperations().entrySet().stream().
                            filter((entry) -> entry.getKey().contains(localhost.getHostName())).
                            collect(Collectors.toList()).isEmpty()
            );

            s.getSocketOperations().entrySet().stream().
                    filter((entry) -> entry.getKey().contains(localhost.getHostName())).
                    findAny().
                    ifPresent((entry) -> {
                            assertEquals(4, entry.getValue().bytesUp.intValue());
                            assertEquals(8, entry.getValue().bytesDown.intValue());
                    });

        }

    }

    @Test
    public void testUninstall() throws Exception {

        SnifferSocketImplFactory.install();
        SnifferSocketImplFactory.uninstall();

        try (Spy<?> s = Sniffer.spy()) {

            Socket socket = new Socket(InetAddress.getByName(null), echoServerRule.getBoundPort());

            assertTrue(socket.isConnected());
            echoServerRule.getCountDownLatch().countDown();
            echoServerRule.getCountDownLatch().await();

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(new byte[]{1, 2, 3, 4});
            outputStream.flush();
            outputStream.close();

            echoServerRule.joinThreads();

            assertTrue(s.getSocketOperations().isEmpty());

        }

    }

}