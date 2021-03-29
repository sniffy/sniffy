package io.sniffy.tls;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import static org.junit.Assert.*;

public class BaseSSLSocketTest {

    private final TemporaryFolder tempFolder = new TemporaryFolder();
    private final EchoSslServerRule echoServerRule = new EchoSslServerRule(tempFolder, RESPONSE, REQUEST.length);

    @Rule
    public TestRule chain = RuleChain
            .outerRule(tempFolder)
            .around(echoServerRule);

    protected final static byte[] RESPONSE = new byte[]{9, 8, 7, 6, 5, 4, 3, 2};
    protected final static byte[] REQUEST = new byte[]{1, 2, 3, 4};

    protected static InetAddress localhost;

    @BeforeClass
    public static void resolveLocalhost() throws UnknownHostException {
        localhost = InetAddress.getByName(null);
    }

    protected void performSocketOperation() {

        try {
            System.out.println(new Date() + " - Connecting to local SSL server"); // TODO: remove
            System.out.flush();

            Socket socket = echoServerRule.getClientSSLContext().getSocketFactory().createSocket(localhost, echoServerRule.getBoundPort());
            socket.setReuseAddress(true);

            assertTrue(socket.isConnected());

            System.out.println(new Date() + " - Sending request to local SSL server"); // TODO: remove
            System.out.flush();

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(REQUEST);
            outputStream.flush();

            System.out.println(new Date() + " - Request sent to local SSL server"); // TODO: remove
            System.out.flush();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream inputStream = socket.getInputStream();
            int read;
            int bytesRead = 0;
            while (bytesRead < RESPONSE.length && (read = inputStream.read()) != -1) {
                baos.write(read);
                bytesRead++;
            }

            System.out.println(new Date() + " - Response received from local SSL server"); // TODO: remove
            System.out.flush();

            echoServerRule.joinThreads();

            System.out.println(new Date() + " - Server threads joined"); // TODO: remove
            System.out.flush();

            socket.close();



            assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
            assertArrayEquals(RESPONSE, baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

}
