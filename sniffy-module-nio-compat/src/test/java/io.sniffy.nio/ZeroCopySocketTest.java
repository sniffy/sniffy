package io.sniffy.nio;

import io.qameta.allure.Issue;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.BaseSocketTest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

import static io.sniffy.Threads.CURRENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ZeroCopySocketTest extends BaseSocketTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    @Issue("issues/414")
    public void testTransferTo() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setSocketCaptureEnabled(true);

        Sniffy.initialize();

        try (Spy<?> s = Sniffy.spy()) {

            try {
                SocketChannel client = SocketChannel.open(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()));

                File newFile = temporaryFolder.newFile();
                {
                    ByteBuffer requestBuffer = ByteBuffer.wrap(BaseSocketTest.REQUEST);
                    RandomAccessFile file = new RandomAccessFile(newFile, "rw");
                    file.getChannel().write(requestBuffer);
                    file.close();
                }

                ByteBuffer responseBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);

                RandomAccessFile file = new RandomAccessFile(newFile, "r");
                file.getChannel().transferTo(0, REQUEST.length, client);

                client.read(responseBuffer);

                client.close();

                echoServerRule.joinThreads();

                Assert.assertArrayEquals(BaseSocketTest.REQUEST, echoServerRule.pollReceivedData());
                Assert.assertArrayEquals(BaseSocketTest.RESPONSE, responseBuffer.array());
            } catch (IOException e) {
                fail(e.getMessage());
            }

            // Current thread socket operations

            assertEquals(1, (long) s.getSocketOperations(CURRENT, true).entrySet().size());

            s.getSocketOperations(CURRENT, true).values().stream().findAny().ifPresent((socketStats) -> {
                Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
            });

        }

    }

    @Test
    @Issue("issues/414")
    public void testDirectBuffer() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setSocketCaptureEnabled(true);

        Sniffy.initialize();

        try (Spy<?> s = Sniffy.spy()) {

            try {
                SocketChannel client = SocketChannel.open(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()));

                ByteBuffer requestBuffer = ByteBuffer.allocateDirect(BaseSocketTest.REQUEST.length);
                requestBuffer.put(ByteBuffer.wrap(BaseSocketTest.REQUEST));
                requestBuffer.flip();

                ByteBuffer responseBuffer = ByteBuffer.allocateDirect(BaseSocketTest.RESPONSE.length);

                client.write(requestBuffer);
                requestBuffer.clear();
                client.read(responseBuffer);

                client.close();

                echoServerRule.joinThreads();

                ByteBuffer responseBufferArrayBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);
                responseBuffer.flip();
                responseBufferArrayBuffer.put(responseBuffer);

                Assert.assertArrayEquals(BaseSocketTest.REQUEST, echoServerRule.pollReceivedData());
                Assert.assertArrayEquals(BaseSocketTest.RESPONSE, responseBufferArrayBuffer.array());
            } catch (IOException e) {
                fail(e.getMessage());
            }

            // Current thread socket operations

            assertEquals(1, (long) s.getSocketOperations(CURRENT, true).entrySet().size());

            s.getSocketOperations(CURRENT, true).values().stream().findAny().ifPresent((socketStats) -> {
                Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
            });

        }

    }

    @Test
    @Issue("issues/414")
    public void testMemoryMappedFile() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setSocketCaptureEnabled(true);

        Sniffy.initialize();

        try (Spy<?> s = Sniffy.spy()) {

            try {
                SocketChannel client = SocketChannel.open(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()));

                File newFile = temporaryFolder.newFile();
                {
                    ByteBuffer requestBuffer = ByteBuffer.wrap(BaseSocketTest.REQUEST);
                    RandomAccessFile file = new RandomAccessFile(newFile, "rw");
                    file.getChannel().write(requestBuffer);
                    file.close();
                }

                ByteBuffer responseBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);

                RandomAccessFile file = new RandomAccessFile(newFile, "r");
                MappedByteBuffer mappedByteBuffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, REQUEST.length);

                client.write(mappedByteBuffer);

                client.read(responseBuffer);

                client.close();

                echoServerRule.joinThreads();

                Assert.assertArrayEquals(BaseSocketTest.REQUEST, echoServerRule.pollReceivedData());
                Assert.assertArrayEquals(BaseSocketTest.RESPONSE, responseBuffer.array());
            } catch (IOException e) {
                fail(e.getMessage());
            }

            // Current thread socket operations

            assertEquals(1, (long) s.getSocketOperations(CURRENT, true).entrySet().size());

            s.getSocketOperations(CURRENT, true).values().stream().findAny().ifPresent((socketStats) -> {
                Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
            });

        }

    }

    @Test
    @Issue("issues/414")
    public void testTransferFrom() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setSocketCaptureEnabled(true);

        Sniffy.initialize();

        try (Spy<?> s = Sniffy.spy()) {

            try {
                SocketChannel client = SocketChannel.open(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()));
                ByteBuffer requestBuffer = ByteBuffer.wrap(BaseSocketTest.REQUEST);
                ByteBuffer responseBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);

                client.write(requestBuffer);
                requestBuffer.clear();

                File newFile = temporaryFolder.newFile();
                {
                    RandomAccessFile file = new RandomAccessFile(newFile, "rw");
                    file.getChannel().transferFrom(client, 0, RESPONSE.length);
                    file.close();
                }
                {
                    RandomAccessFile file = new RandomAccessFile(newFile, "r");
                    file.getChannel().read(responseBuffer);
                    file.close();
                }

                client.close();

                echoServerRule.joinThreads();

                Assert.assertArrayEquals(BaseSocketTest.REQUEST, echoServerRule.pollReceivedData());
                Assert.assertArrayEquals(BaseSocketTest.RESPONSE, responseBuffer.array());
            } catch (IOException e) {
                fail(e.getMessage());
            }

            // Current thread socket operations

            assertEquals(1, (long) s.getSocketOperations(CURRENT, true).entrySet().size());

            s.getSocketOperations(CURRENT, true).values().stream().findAny().ifPresent((socketStats) -> {
                Assert.assertEquals(BaseSocketTest.REQUEST.length, socketStats.bytesUp.intValue());
                Assert.assertEquals(BaseSocketTest.RESPONSE.length, socketStats.bytesDown.intValue());
            });

        }

    }

}