package io.sniffy.tls;

import io.sniffy.socket.BaseSocketTest;
import io.sniffy.util.ReflectionUtil;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Issue;
import sun.security.jca.Providers;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Security;

import static org.junit.Assert.*;

public class SniffySSLSocketFactoryTest extends BaseSocketTest {

    public static class TestSSLSocketFactory extends SSLSocketFactory {

        @Override
        public String[] getDefaultCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return new String[0];
        }

        @Override
        public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
            return null;
        }

        @Override
        public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket() throws IOException {
            return null;
        }

    }

    @Test
    @Issue("issues/439")
    public void testExistingSSLSocketFactoryWasCreateViaSecurityProperties() throws Exception {

        ReflectionUtil.setFields(SSLSocketFactory.class, null, SSLSocketFactory.class, null);
        ReflectionUtil.setFirstField(SSLSocketFactory.class, null, Boolean.TYPE, false);

        Security.setProperty("ssl.SocketFactory.provider", TestSSLSocketFactory.class.getName());
        SSLSocketFactory.getDefault();
        assertEquals(TestSSLSocketFactory.class, SSLSocketFactory.getDefault().getClass());
        assertNull(SSLSocketFactory.getDefault().createSocket());

        try {
            SniffyTlsModule.initialize();

            Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());

            assertTrue(socket instanceof SniffySSLSocket);

        } finally {
            SniffyTlsModule.uninstall();

            Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());

            assertFalse(socket instanceof SniffySSLSocket);
            assertNull(SSLSocketFactory.getDefault().createSocket());
        }

    }

    @Test
    public void testCreateSocket() throws Exception {

        try {
            SniffyTlsModule.initialize();

            Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());

            assertTrue(socket instanceof SniffySSLSocket);

        } finally {
            SniffyTlsModule.uninstall();

            Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());

            assertFalse(socket instanceof SniffySSLSocket);
        }

    }

}