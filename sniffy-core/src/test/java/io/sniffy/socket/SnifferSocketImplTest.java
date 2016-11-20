package io.sniffy.socket;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketImpl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SocketImpl.class)
public class SnifferSocketImplTest {

    @Mock
    private SocketImpl delegate;

    private SnifferSocketImpl sniffySocket;

    @Before
    public void createSniffySocket() {
        sniffySocket = new SnifferSocketImpl(delegate);
    }

    @Test
    public void testSendUrgentData() throws Exception {

        sniffySocket.sendUrgentData(1);

        verifyPrivate(delegate).invoke("sendUrgentData",1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testSendUrgentDataThrowsIOException() throws Exception {

        IOException expected = new IOException();
        when(delegate, "sendUrgentData", anyInt()).thenThrow(expected);

        try {
            sniffySocket.sendUrgentData(1);
            fail();
        } catch (IOException actual) {
            assertEquals(expected, actual);
        }

        verifyPrivate(delegate).invoke("sendUrgentData",1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testSendUrgentDataThrowsRuntimeException() throws Exception {

        RuntimeException expected = new RuntimeException();
        when(delegate, "sendUrgentData", anyInt()).thenThrow(expected);

        try {
            sniffySocket.sendUrgentData(1);
            fail();
        } catch (Exception actual) {
            assertEquals(expected, actual);
        }

        verifyPrivate(delegate).invoke("sendUrgentData",1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInput() throws Exception {

        sniffySocket.shutdownInput();

        verifyPrivate(delegate).invoke("shutdownInput");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInputThrowsIOException() throws Exception {

        IOException expected = new IOException();
        when(delegate, "shutdownInput").thenThrow(expected);

        try {
            sniffySocket.shutdownInput();
            fail();
        } catch (IOException actual) {
            assertEquals(expected, actual);
        }

        verifyPrivate(delegate).invoke("shutdownInput");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInputThrowsRuntimeException() throws Exception {

        RuntimeException expected = new RuntimeException();
        when(delegate, "shutdownInput").thenThrow(expected);

        try {
            sniffySocket.shutdownInput();
            fail();
        } catch (Exception actual) {
            assertEquals(expected, actual);
        }

        verifyPrivate(delegate).invoke("shutdownInput");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownOutput() throws Exception {

        sniffySocket.shutdownOutput();

        verifyPrivate(delegate).invoke("shutdownOutput");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testGetFileDescriptor() throws Exception {

        FileDescriptor expected = mock(FileDescriptor.class);

        when(delegate, "getFileDescriptor").thenReturn(expected);

        FileDescriptor actual = sniffySocket.getFileDescriptor();

        verifyPrivate(delegate).invoke("getFileDescriptor");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testGetInetAddress() throws Exception {

        InetAddress expected = mock(InetAddress.class);

        when(delegate, "getInetAddress").thenReturn(expected);

        InetAddress actual = sniffySocket.getInetAddress();

        verifyPrivate(delegate).invoke("getInetAddress");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testGetPort() throws Exception {

        int expected = 1;

        when(delegate, "getPort").thenReturn(expected);

        int actual = sniffySocket.getPort();

        verifyPrivate(delegate).invoke("getPort");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testSupportsUrgentData() throws Exception {

        boolean expected = true;

        when(delegate, "supportsUrgentData").thenReturn(expected);

        boolean actual = sniffySocket.supportsUrgentData();

        verifyPrivate(delegate).invoke("supportsUrgentData");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testGetLocalPort() throws Exception {

        int expected = 42;

        when(delegate, "getLocalPort").thenReturn(expected);

        int actual = sniffySocket.getLocalPort();

        verifyPrivate(delegate).invoke("getLocalPort");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testSetPerformancePreferences() throws Exception {

        sniffySocket.setPerformancePreferences(1, 2, 3);

        verifyPrivate(delegate).invoke("setPerformancePreferences", 1, 2, 3);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testCreate() throws Exception {

        sniffySocket.create(true);

        verifyPrivate(delegate).invoke("create", true);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testConnect() throws Exception {

        sniffySocket.connect("localhost", 123);

        verifyPrivate(delegate).invoke("connect", "localhost", 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testConnectInetAddress() throws Exception {

        InetAddress inetAddress = InetAddress.getLoopbackAddress();

        sniffySocket.connect(inetAddress, 123);

        verifyPrivate(delegate).invoke("connect", inetAddress, 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testConnectSocketAddress() throws Exception {

        SocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 123);

        sniffySocket.connect(socketAddress, 123);

        verifyPrivate(delegate).invoke("connect", socketAddress, 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testBindInetAddress() throws Exception {

        InetAddress inetAddress = InetAddress.getLoopbackAddress();

        sniffySocket.bind(inetAddress, 123);

        verifyPrivate(delegate).invoke("bind", inetAddress, 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testListen() throws Exception {

        sniffySocket.listen(123);

        verifyPrivate(delegate).invoke("listen", 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testAccept() throws Exception {

        SocketImpl socketImpl = new SnifferSocketImpl(null);

        sniffySocket.accept(socketImpl);

        verifyPrivate(delegate).invoke("accept", socketImpl);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testGetInputStream() throws Exception {

        InputStream expected = new ByteArrayInputStream(new byte[]{1,2,3});

        when(delegate, "getInputStream").thenReturn(expected);

        InputStream actual = sniffySocket.getInputStream();

        verifyPrivate(delegate).invoke("getInputStream");
        verifyNoMoreInteractions(delegate);

        assertEquals(SnifferInputStream.class, actual.getClass());
        assertEquals(1, actual.read());

    }

    @Test
    public void testGetOutputStream() throws Exception {

        ByteArrayOutputStream expected = new ByteArrayOutputStream();

        when(delegate, "getOutputStream").thenReturn(expected);

        OutputStream actual = sniffySocket.getOutputStream();

        verifyPrivate(delegate).invoke("getOutputStream");
        verifyNoMoreInteractions(delegate);

        assertEquals(SnifferOutputStream.class, actual.getClass());
        actual.write(1);

        assertArrayEquals(new byte[]{1}, expected.toByteArray());

    }

    @Test
    public void testAvailable() throws Exception {

        int expected = 1;

        when(delegate, "available").thenReturn(expected);

        int actual = sniffySocket.available();

        verifyPrivate(delegate).invoke("available");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testClose() throws Exception {

        sniffySocket.close();

        verifyPrivate(delegate).invoke("close");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testSetOption() throws Exception {

        int optId = 1;
        Object option = new Object();

        sniffySocket.setOption(optId, option);

        verifyPrivate(delegate).invoke("setOption", optId, option);
        verifyNoMoreInteractions(delegate);

    }


    @Test
    public void testGetOption() throws Exception {

        Object expected = new Object();

        when(delegate, "getOption", 1).thenReturn(expected);

        Object actual = sniffySocket.getOption(1);

        verifyPrivate(delegate).invoke("getOption", 1);
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testToString() throws Exception {

        String expected = "expected";

        when(delegate, "toString").thenReturn(expected);

        String actual = sniffySocket.toString();

        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

}
