package io.sniffy.socket;

import io.sniffy.Sniffer;
import io.sniffy.Spy;
import io.sniffy.Threads;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static io.sniffy.Threads.CURRENT;
import static org.junit.Assert.*;

public class SnifferSocketImplFactoryTest {

    private final static byte[] RESPONSE = new byte[]{9,8,7,6,5,4,3,2};
    private final static byte[] REQUEST = new byte[]{1, 2, 3, 4};

    private static InetAddress localhost;

    @BeforeClass
    public static void resolveLocalhost() throws UnknownHostException {
        localhost = InetAddress.getByName(null);
    }

    @Rule
    public EchoServerRule echoServerRule = new EchoServerRule(RESPONSE);

    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.install();

        try (Spy<?> s = Sniffer.spy()) {

            performSocketOperation();

            Thread thread = new Thread(this::performSocketOperation);
            thread.start();
            thread.join();

            // Current thread socket operations

            assertFalse(
                    s.getSocketOperations(CURRENT).entrySet().stream().
                            filter((entry) -> entry.getKey().address.getAddress().equals(localhost)).
                            collect(Collectors.toList()).isEmpty()
            );

            assertEquals(
                    1,
                    s.getSocketOperations(CURRENT).entrySet().stream().
                            filter((entry) -> entry.getKey().address.getAddress().equals(localhost)).
                            map((entry) -> new SocketMetaData(entry.getKey().address, entry.getKey().connectionId, null, entry.getKey().owner)).
                            collect(Collectors.toSet()).size()
            );

            s.getSocketOperations(CURRENT).entrySet().stream().
                    filter((entry) -> entry.getKey().address.getAddress().equals(localhost)).
                    map(Map.Entry::getValue).
                    reduce(SocketStats::combine).
                    ifPresent((socketStats) -> {
                            assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                            assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
                    });

            // Other threads socket operations

            assertFalse(
                    s.getSocketOperations(Threads.OTHERS).entrySet().stream().
                            filter((entry) -> entry.getKey().address.getAddress().equals(localhost)).
                            collect(Collectors.toList()).isEmpty()
            );
            assertEquals(
                    1,
                    s.getSocketOperations(Threads.OTHERS).entrySet().stream().
                            filter((entry) -> entry.getKey().address.getAddress().equals(localhost)).
                            map((entry) -> new SocketMetaData(entry.getKey().address, entry.getKey().connectionId, null, entry.getKey().owner)).
                            collect(Collectors.toSet()).size()
            );

            s.getSocketOperations(Threads.OTHERS).entrySet().stream().
                    filter((entry) -> entry.getKey().address.getAddress().equals(localhost)).
                    map(Map.Entry::getValue).
                    reduce(SocketStats::combine).
                    ifPresent((socketStats) -> {
                        assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                        assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
                    });

            // Any threads socket operations

            assertFalse(
                    s.getSocketOperations(Threads.ANY).entrySet().stream().
                            filter((entry) -> entry.getKey().address.getAddress().equals(localhost)).
                            collect(Collectors.toList()).isEmpty()
            );
            assertEquals(
                    2,
                    s.getSocketOperations(Threads.ANY).entrySet().stream().
                            filter((entry) -> entry.getKey().address.getAddress().equals(localhost)).
                            map((entry) -> new SocketMetaData(entry.getKey().address, entry.getKey().connectionId, null, entry.getKey().owner)).
                            collect(Collectors.toSet()).size()
            );

            Map<SocketMetaData, SocketStats> collect = s.getSocketOperations(Threads.ANY).entrySet().stream().collect(
                    Collectors.groupingBy(
                            (entry) -> new SocketMetaData(entry.getKey().address, entry.getKey().connectionId, null, entry.getKey().owner),
                            Collectors.mapping(Map.Entry::getValue, Collector.of(
                                    SocketStats::new, SocketStats::accumulate, SocketStats::combine
                            ))
                    )
            );
            collect.entrySet().stream().forEach((entry) -> {
                            assertEquals(REQUEST.length, entry.getValue().bytesUp.intValue());
                            assertEquals(RESPONSE.length, entry.getValue().bytesDown.intValue());
                    });

        } finally {
            SnifferSocketImplFactory.uninstall();
        }

    }

    @Test
    public void testUninstall() throws Exception {

        SnifferSocketImplFactory.install();
        SnifferSocketImplFactory.uninstall();

        try (Spy<?> s = Sniffer.spy()) {

            performSocketOperation();

            assertTrue(s.getSocketOperations(CURRENT).isEmpty());

        }

    }

    private void performSocketOperation() {

        try {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}