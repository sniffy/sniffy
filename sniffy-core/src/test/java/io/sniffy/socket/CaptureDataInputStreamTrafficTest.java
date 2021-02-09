package io.sniffy.socket;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CaptureDataInputStreamTrafficTest {


    protected final static byte[] RESPONSE;
    protected final static byte[] REQUEST;

    protected static InetAddress localhost;

    static {

        byte[] request = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(42);
            dos.writeLong(100500L);
            dos.flush();
            dos.close();
            request = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        REQUEST = request;

        byte[] response = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(1197569125);
            dos.writeUTF("Hello, World!");
            dos.flush();
            dos.close();
            response = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        RESPONSE = response;
    }

    @Rule
    public ConversationServerRule echoServerRule = new ConversationServerRule(
            Arrays.asList(REQUEST, REQUEST),
            Arrays.asList(RESPONSE, RESPONSE)
    );

    @BeforeClass
    public static void resolveLocalhost() throws UnknownHostException {
        localhost = InetAddress.getByName(null);
    }

    @Test
    @Stories({"issues/415"})
    public void testCaptureInputStream() throws Exception {

        SniffyConfiguration.INSTANCE.setCaptureTraffic(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        Sniffy.initialize();

        long prevTimeStamp = System.currentTimeMillis();

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            try {
                Socket socket = new Socket(localhost, echoServerRule.getBoundPort());
                socket.setTcpNoDelay(true);
                socket.setReuseAddress(true);

                assertTrue(socket.isConnected());

                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();

                for (int i = 0; i < 2; i++) {

                    DataOutputStream dos = new DataOutputStream(outputStream);
                    dos.writeInt(42);
                    dos.writeLong(100500L);
                    dos.flush();

                    DataInputStream dis = new DataInputStream(inputStream);
                    int param1 = dis.readInt();
                    String param2 = dis.readUTF();
                    assertEquals(1197569125, param1);
                    assertEquals("Hello, World!", param2);

                }

                socket.shutdownInput();
                socket.shutdownOutput();

                echoServerRule.joinThreads();

                assertEquals(REQUEST.length * 2, echoServerRule.getBytesReceived());
            } catch (IOException e) {
                fail(e.getMessage());
            }

            Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic(
                    Threads.ANY,
                    AddressMatchers.exactAddressMatcher(localhost.getHostAddress()),
                    GroupingOptions.builder().groupByStackTrace(false).build()
            );

            for (Map.Entry<SocketMetaData, List<NetworkPacket>> entry : networkTraffic.entrySet()) {

                SocketMetaData socketMetaData = entry.getKey();

                Protocol protocol = socketMetaData.getProtocol(); // say TCP
                String hostName = socketMetaData.getAddress().getHostName(); // say "hostname.acme.com"
                int port = socketMetaData.getAddress().getPort(); // say 443

                assertEquals(Protocol.TCP, protocol);
                assertEquals(localhost.getHostName(), hostName);
                assertEquals(echoServerRule.getBoundPort(), port);

                String stackTrace = socketMetaData.getStackTrace();// optional stacktrace for operation as a String
                ThreadMetaData threadMetaData = socketMetaData.getThreadMetaData();// information about thread which performed the operation

                assertNull(stackTrace);
                assertNull(threadMetaData);

                boolean nextPacketMustBeSent = true;

                List<NetworkPacket> networkPackets = entry.getValue();

                assertEquals(4, networkPackets.size());

                for (NetworkPacket networkPacket : networkPackets) {

                    long timestamp = networkPacket.getTimestamp(); // timestamp of operation
                    byte[] data = networkPacket.getBytes(); // captured traffic

                    assertTrue(timestamp >= prevTimeStamp);
                    prevTimeStamp = timestamp;

                    assertEquals(nextPacketMustBeSent, networkPacket.isSent());

                    if (nextPacketMustBeSent) {
                        assertArrayEquals(REQUEST, data);
                    } else {
                        assertArrayEquals(RESPONSE, data);
                    }

                    nextPacketMustBeSent = !nextPacketMustBeSent;

                }

            }

        }

    }

}
