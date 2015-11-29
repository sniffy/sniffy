package io.sniffy.socket;

import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SnifferSocketImplFactoryTest {

    @Rule
    public EchoServerRule echoServerRule = new EchoServerRule();

    @Test
    public void testInstall() throws Exception {

        //SnifferSocketImplFactory.install();

        Socket socket = new Socket(InetAddress.getByName(null), echoServerRule.getBoundPort());
        OutputStream outputStream = socket.getOutputStream();

        assertTrue(socket.isConnected());

        Lock isObtainedLock = new ReentrantLock();
        Condition isObtainedCondition = isObtainedLock.newCondition();
        AtomicBoolean isObtained = new AtomicBoolean();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Thread readThread = new Thread(() -> {
            try {

                InputStream inputStream;

                try {
                    isObtainedLock.lock();
                    inputStream = socket.getInputStream();
                    isObtained.set(true);
                    isObtainedCondition.signalAll();
                } finally {
                    isObtainedLock.unlock();
                }

                byte[] buff = new byte[1024];
                int read;
                while ((read = inputStream.read(buff)) != -1) {
                    baos.write(buff, 0, read);
                }
                System.out.println("EchoInputStream read");

            } catch (SocketException e) {
                if (!"socket closed".equalsIgnoreCase(e.getMessage())) {
                    e.printStackTrace();
                    fail();
                }
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        });

        readThread.start();

        byte[] message = {1, 2, 3, 4};

        outputStream.write(message);
        outputStream.flush();

        System.out.println("ClientOutputStream flushed");

        try {
            isObtainedLock.lock();
            if (!isObtained.get())
                isObtainedCondition.await();
        } finally {
            isObtainedLock.unlock();
        }

        outputStream.close();

        readThread.join();

        assertArrayEquals(message, baos.toByteArray());

    }

}