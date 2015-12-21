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

        SnifferSocketImplFactory.install();

        Socket socket = new Socket(InetAddress.getByName(null), echoServerRule.getBoundPort());
        assertTrue(socket.isConnected());

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(new byte[]{1, 2, 3, 4});
        outputStream.flush();
        outputStream.close();

        echoServerRule.joinThreads();

    }

}