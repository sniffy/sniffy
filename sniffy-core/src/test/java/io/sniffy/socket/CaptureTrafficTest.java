package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.ThreadMetaData;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Stories;

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
    @Stories({"issues/400", "issues/401"})
    public void testTrafficCapture() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        long prevTimeStamp = System.currentTimeMillis();

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {

            performSocketOperation();

            Thread thread = new Thread(this::performSocketOperation);
            thread.start();
            thread.join();

            Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic();

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
    @Stories({"issues/400", "issues/401"})
    public void testTrafficCaptureDisabled() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(false).build())) {

            assertTrue(spy.getNetworkTraffic().isEmpty());

        }

    }

    @Test
    @Stories({"issues/411"})
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
    @Stories({"issues/411"})
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
            outputStream.write(REQUEST, 0, REQUEST.length - 1);
            outputStream.flush();
            System.err.println("Flushed part of request");
            System.err.flush();
            socket.sendUrgentData(REQUEST[REQUEST.length - 1]);
            System.err.println("Sent urgent data");
            System.err.flush();
            outputStream.flush();

            System.err.println("Outputstream flushed");
            System.err.flush();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream inputStream = socket.getInputStream();
            int read;
            while ((read = inputStream.read()) != -1) {
                baos.write(read);
            }
            socket.shutdownInput();

            System.err.println("Input shut down");
            System.err.flush();


            socket.shutdownOutput();

            System.err.println("Output shut down");
            System.err.flush();

            outputStream.close();

            System.err.println("Output closed");
            System.err.flush();

            inputStream.close();

            System.err.println("Input closed");
            System.err.flush();

            socket.close();

            System.err.println("Socket closed");
            System.err.flush();

            echoServerRule.joinThreads();

            assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
            assertArrayEquals(RESPONSE, baos.toByteArray());

            Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic();

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
    @Stories({"issues/411"})
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
    @Stories({"issues/411"})
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
