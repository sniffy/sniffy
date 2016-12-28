package io.sniffy.socket;

import org.junit.BeforeClass;
import org.junit.Rule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

public class BaseSocketTest {

    protected final static byte[] RESPONSE = new byte[]{9,8,7,6,5,4,3,2};
    protected final static byte[] REQUEST = new byte[]{1, 2, 3, 4};

    protected static InetAddress localhost;

    @Rule
    public EchoServerRule echoServerRule = new EchoServerRule(RESPONSE);

    @BeforeClass
    public static void resolveLocalhost() throws UnknownHostException {
        localhost = InetAddress.getByName(null);
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
            }
            socket.shutdownInput();

            echoServerRule.joinThreads();

            assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
            assertArrayEquals(RESPONSE, baos.toByteArray());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}
