package io.sniffy.socket;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
    public void testSendUrgentDataThrowIOException() throws Exception {

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
    public void testSendUrgentDataThrowRuntimeException() throws Exception {

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


}
