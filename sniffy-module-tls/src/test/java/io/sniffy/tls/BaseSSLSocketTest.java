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
            Socket socket = echoServerRule.getClientSSLContext().getSocketFactory().createSocket(localhost, echoServerRule.getBoundPort());
            socket.setReuseAddress(true);

            assertTrue(socket.isConnected());

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(REQUEST);
            outputStream.flush();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream inputStream = socket.getInputStream();
            int read;
            int bytesRead = 0;
            while (bytesRead < RESPONSE.length && (read = inputStream.read()) != -1) {
                baos.write(read);
                bytesRead++;
            }

            echoServerRule.joinThreads();

            socket.close();

            assertArrayEquals(RESPONSE, baos.toByteArray());
            assertArrayEquals(REQUEST, echoServerRule.pollReceivedData());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

}
