package io.sniffy.socket;

import io.sniffy.Sniffer;
import io.sniffy.Spy;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SnifferSocketImplFactoryTest {

    private final static byte[] RESPONSE = new byte[]{9,8,7,6,5,4,3,2};
    private final static byte[] REQUEST = new byte[]{1, 2, 3, 4};

    @Rule
    public EchoServerRule echoServerRule = new EchoServerRule(RESPONSE);

    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.install();

        try (Spy<?> s = Sniffer.spy()) {

            InetAddress localhost = InetAddress.getByName(null);
            Socket socket = new Socket(localhost, echoServerRule.getBoundPort());
            socket.setReuseAddress(true);

            assertTrue(socket.isConnected());

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(REQUEST);
            outputStream.flush();
            socket.shutdownOutput();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream inputStream = socket.getInputStream();
            int read;
            while ((read = inputStream.read()) != -1) {
                baos.write(read);
            }
            socket.shutdownInput();

            echoServerRule.joinThreads();

            assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
            assertArrayEquals(RESPONSE, baos.toByteArray());

            assertFalse(
                    s.getSocketOperations().entrySet().stream().
                            filter((entry) -> entry.getKey().contains(localhost.getHostName())).
                            collect(Collectors.toList()).isEmpty()
            );

            s.getSocketOperations().entrySet().stream().
                    filter((entry) -> entry.getKey().contains(localhost.getHostName())).
                    findAny().
                    ifPresent((entry) -> {
                            assertEquals(REQUEST.length, entry.getValue().bytesUp.intValue());
                            assertEquals(RESPONSE.length, entry.getValue().bytesDown.intValue());
                    });

        }

    }

    @Test
    public void testUninstall() throws Exception {

        SnifferSocketImplFactory.install();
        SnifferSocketImplFactory.uninstall();

        try (Spy<?> s = Sniffer.spy()) {

            InetAddress localhost = InetAddress.getByName(null);
            Socket socket = new Socket(localhost, echoServerRule.getBoundPort());
            socket.setReuseAddress(true);

            assertTrue(socket.isConnected());

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(REQUEST);
            outputStream.flush();
            socket.shutdownOutput();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream inputStream = socket.getInputStream();
            int read;
            while ((read = inputStream.read()) != -1) {
                baos.write(read);
            }
            socket.shutdownInput();

            echoServerRule.joinThreads();

            assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
            assertArrayEquals(RESPONSE, baos.toByteArray());

            assertTrue(s.getSocketOperations().isEmpty());

        }

    }

}