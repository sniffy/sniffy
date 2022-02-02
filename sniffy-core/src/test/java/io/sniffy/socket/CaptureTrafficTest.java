package io.sniffy.socket;

import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.ThreadMetaData;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.util.OSUtil;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CaptureTrafficTest extends BaseSocketTest {

    @Test
    @Stories({@Story("issues/400"), @Story("issues/401")})
    public void testTrafficCapture() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        long prevTimeStamp = System.currentTimeMillis();

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {

            performSocketOperation();

            Thread thread = new Thread(this::performSocketOperation);
            thread.start();
            thread.join();

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

    @Test
    @Stories({@Story("issues/400"), @Story("issues/401")})
    public void testTrafficCaptureDisabled() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(false).build())) {

            assertTrue(spy.getNetworkTraffic().isEmpty());

        }

    }

    @Test
    @Story("issues/411")
    public void testCaptureInputStream() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        long prevTimeStamp = System.currentTimeMillis();

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {

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

                {
                    byte[] fourBytesBuff = new byte[4];
                    int numRead = inputStream.read(fourBytesBuff);
                    assertTrue(numRead > 1);
                    baos.write(fourBytesBuff, 0, numRead);
                }

                {
                    byte[] fourBytesBuff = new byte[4];
                    int numRead = inputStream.read(fourBytesBuff, 1, 2);
                    assertTrue(numRead > 1);
                    baos.write(fourBytesBuff, 1, numRead);
                }

                int read;
                while ((read = inputStream.read()) != -1) {
                    baos.write(read);
                }
                socket.shutdownInput();

                echoServerRule.joinThreads();

                assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
                assertArrayEquals(RESPONSE, baos.toByteArray());
            } catch (IOException e) {
                fail(e.getMessage());
            }

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
    @Story("issues/411")
    public void testCaptureInputStream_SendUrgentData() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        long prevTimeStamp = System.currentTimeMillis();

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {

            Socket socket = new Socket(localhost, echoServerRule.getBoundPort());
            socket.setReuseAddress(true);

            socket.setOOBInline(true);
            assertTrue(socket.getOOBInline());

            assertTrue(socket.isConnected());

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(REQUEST, 0, REQUEST.length - 2);
            outputStream.flush();
            socket.sendUrgentData(REQUEST[REQUEST.length - 2]);
            outputStream.write(REQUEST, REQUEST.length - 1, 1);
            outputStream.flush();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream inputStream = socket.getInputStream();
            int read;
            while ((read = inputStream.read()) != -1) {
                baos.write(read);
            }

            inputStream.close();
            outputStream.close();

            socket.close();

            echoServerRule.joinThreads();

            // On MacOS urgent data seems to be lost intermittently
            // TODO: investigate why urgent data is lost on MacOS
            if (!OSUtil.isMac()) {
                assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
            }

            assertArrayEquals(RESPONSE, baos.toByteArray());

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
    @Story("issues/411")
    public void testCaptureInputStream_Skip() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        long prevTimeStamp = System.currentTimeMillis();

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {

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

                {
                    byte[] fourBytesBuff = new byte[2];
                    int numRead = inputStream.read(fourBytesBuff);
                    assertTrue(numRead > 1);
                    baos.write(fourBytesBuff, 0, numRead);
                }

                assertEquals(2, inputStream.skip(2));

                {
                    byte[] fourBytesBuff = new byte[4];
                    int numRead = inputStream.read(fourBytesBuff, 1, 2);
                    assertTrue(numRead > 1);
                    baos.write(fourBytesBuff, 1, numRead);
                }

                int read;
                while ((read = inputStream.read()) != -1) {
                    baos.write(read);
                }
                socket.shutdownInput();

                echoServerRule.joinThreads();

                assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
            } catch (IOException e) {
                fail(e.getMessage());
            }

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
    @Story("issues/411")
    public void testCaptureInputStreamWithoutStackTraces() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        long prevTimeStamp = System.currentTimeMillis();

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(false).build())) {

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

                {
                    byte[] fourBytesBuff = new byte[4];
                    int numRead = inputStream.read(fourBytesBuff);
                    assertTrue(numRead > 1);
                    baos.write(fourBytesBuff, 0, numRead);
                }

                {
                    byte[] fourBytesBuff = new byte[4];
                    int numRead = inputStream.read(fourBytesBuff, 1, 2);
                    assertTrue(numRead > 1);
                    baos.write(fourBytesBuff, 1, numRead);
                }

                int read;
                while ((read = inputStream.read()) != -1) {
                    baos.write(read);
                }
                socket.shutdownInput();

                echoServerRule.joinThreads();

                assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
                assertArrayEquals(RESPONSE, baos.toByteArray());
            } catch (IOException e) {
                fail(e.getMessage());
            }

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

}
