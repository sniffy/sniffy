package io.sniffy.socket;

import io.qameta.allure.Issue;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.ThreadMetaData;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CaptureBufferedTrafficTest {

    protected final static byte[] RESPONSE = new byte[]{9, 8, 7, 6, 5, 4, 3, 2};

    protected final static byte[] REQUEST = new byte[]{1, 2, 3, 4};

    protected static InetAddress localhost;

    @Rule
    public EchoServerRule echoServerRule = new EchoServerRule(RESPONSE);

    @BeforeClass
    public static void resolveLocalhost() throws UnknownHostException {
        localhost = InetAddress.getByName(null);
        SniffyConfiguration.INSTANCE.setPacketMergeThreshold(0);
    }

    @BeforeClass
    public static void setPacketMergeThresholdToZero() {
        SniffyConfiguration.INSTANCE.setPacketMergeThreshold(0);
    }

    @AfterClass
    public static void resetPacketMergeThreshold() {
        SniffyConfiguration.INSTANCE.setPacketMergeThreshold(500);
    }

    protected void performSocketOperation() {

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
                //noinspection BusyWait
                Thread.sleep(2);
            }
            socket.shutdownInput();

            echoServerRule.joinThreads();

            assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
            assertArrayEquals(RESPONSE, baos.toByteArray());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    @Issue("issues/528")
    public void testTrafficCaptureBufferInputStream() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        long prevTimeStamp = System.currentTimeMillis();

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).bufferIncomingTraffic(true).build())) {

            performSocketOperation();

            Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic();

            assertEquals(1, networkTraffic.size());

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

                assertEquals(2, networkPackets.size());

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

    @Test
    @Issue("issues/528")
    public void testTrafficCaptureUnbufferedInputStream() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {

            performSocketOperation();

            Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic();

            assertEquals(1, networkTraffic.size());

            for (Map.Entry<SocketMetaData, List<NetworkPacket>> entry : networkTraffic.entrySet()) {

                List<NetworkPacket> networkPackets = entry.getValue();
                assertTrue(networkPackets.size() > 2);

            }

        }

    }

}
