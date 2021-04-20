package io.sniffy.socket;

import io.sniffy.registry.ConnectionsRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SnifferSocketImplTest {

    @Mock
    private PublicSocketImpl delegate;

    @Mock
    private Sleep sleep;

    private SnifferSocketImpl sniffySocket;

    @Before
    public void createSniffySocket() throws Exception {
        sniffySocket = new SnifferSocketImpl(delegate, sleep);

        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void testSendUrgentData() throws Exception {

        sniffySocket.sendUrgentData(1);

        // TODO: insert timeout here and to similar methods?

        verify(delegate).sendUrgentData(1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testSendUrgentDataThrowsIOException() throws Exception {

        IOException expected = new IOException();
        doThrow(expected).when(delegate).sendUrgentData(anyInt());

        try {
            sniffySocket.sendUrgentData(1);
            fail();
        } catch (IOException actual) {
            assertEquals(expected, actual);
        }

        verify(delegate).sendUrgentData(1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testSendUrgentDataThrowsRuntimeException() throws Exception {

        RuntimeException expected = new RuntimeException();
        doThrow(expected).when(delegate).sendUrgentData(anyInt());

        try {
            sniffySocket.sendUrgentData(1);
            fail();
        } catch (Exception actual) {
            assertEquals(expected, actual);
        }

        verify(delegate).sendUrgentData(1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInput() throws Exception {

        sniffySocket.shutdownInput();

        verify(delegate).shutdownInput();
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInputThrowsIOException() throws Exception {

        IOException expected = new IOException();
        doThrow(expected).when(delegate).shutdownInput();

        try {
            sniffySocket.shutdownInput();
            fail();
        } catch (IOException actual) {
            assertEquals(expected, actual);
        }

        verify(delegate).shutdownInput();
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInputThrowsRuntimeException() throws Exception {

        RuntimeException expected = new RuntimeException();
        doThrow(expected).when(delegate).shutdownInput();

        try {
            sniffySocket.shutdownInput();
            fail();
        } catch (Exception actual) {
            assertEquals(expected, actual);
        }

        verify(delegate).shutdownInput();
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownOutput() throws Exception {

        sniffySocket.shutdownOutput();

        verify(delegate).shutdownOutput();
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testGetFileDescriptor() throws Exception {

        FileDescriptor expected = new FileDescriptor();

        when(delegate.getFileDescriptor()).thenReturn(expected);

        FileDescriptor actual = sniffySocket.getFileDescriptor();

        verify(delegate).getFileDescriptor();
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testGetInetAddress() throws Exception {

        InetAddress expected = mock(InetAddress.class);

        when(delegate.getInetAddress()).thenReturn(expected);

        InetAddress actual = sniffySocket.getInetAddress();

        verify(delegate).getInetAddress();
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testGetPort() throws Exception {

        int expected = 1;

        when(delegate.getPort()).thenReturn(expected);

        int actual = sniffySocket.getPort();

        verify(delegate).getPort();
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testSupportsUrgentData() throws Exception {

        when(delegate.supportsUrgentData()).thenReturn(true);

        boolean actual = sniffySocket.supportsUrgentData();

        verify(delegate).supportsUrgentData();
        verifyNoMoreInteractions(delegate);

        assertTrue(actual);

    }

    @Test
    public void testGetLocalPort() throws Exception {

        int expected = 42;

        when(delegate.getLocalPort()).thenReturn(expected);

        int actual = sniffySocket.getLocalPort();

        verify(delegate).getLocalPort();
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testSetPerformancePreferences() throws Exception {

        sniffySocket.setPerformancePreferences(1, 2, 3);

        verify(delegate).setPerformancePreferences(eq(1), eq(2), eq(3));
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testCreate() throws Exception {

        sniffySocket.create(true);

        verify(delegate).create(eq(true));
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testConnect() throws Exception {

        sniffySocket.connect("localhost", 123);

        verify(delegate).connect(eq("localhost"), eq(123));
        verifyNoMoreInteractions(delegate);

    }

    @Test
    @Features({"issues/219"})
    public void testConnectWithDelay() throws Exception {

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 123, 10);

        doNothing().when(sleep).doSleep(anyInt());

        sniffySocket.connect("localhost", 123);

        verify(sleep).doSleep(eq(10));
        verify(delegate).connect(eq("localhost"), eq(123));

        verifyNoMoreInteractions(delegate);

    }

    @Test
    @Features({"issues/219"})
    public void testConnectWithDelayException() throws Exception {

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 123, -10);

        doNothing().when(sleep).doSleep(anyInt());

        try {
            sniffySocket.connect("localhost", 123);
            fail();
        } catch (ConnectException e) {
            assertNotNull(e);
        }

        verify(sleep).doSleep(eq(10));

        verifyNoMoreInteractions(delegate);

    }

    @Test
    @Features({"issues/219"})
    public void testConnectWithDelayThreadSleeps() throws Exception {

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 123, 1000);

        doCallRealMethod().when(sleep).doSleep(anyInt());

        AtomicReference<Exception> exceptionReference = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                sniffySocket.connect("localhost", 123);
            } catch (IOException e) {
                exceptionReference.set(e);
            }
        });

        assertNull(exceptionReference.get());

        thread.start();
        Thread.sleep(500);

        assertEquals(Thread.State.TIMED_WAITING, thread.getState());

        thread.join(1000);

        verify(delegate).connect(eq("localhost"), eq(123));
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testConnectInetAddress() throws Exception {

        InetAddress inetAddress = InetAddress.getLoopbackAddress();

        sniffySocket.connect(inetAddress, 123);

        verify(delegate).connect(eq(inetAddress), eq(123));

        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testConnectSocketAddress() throws Exception {

        SocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 123);

        sniffySocket.connect(socketAddress, 123);

        verify(delegate).connect(eq(socketAddress), eq(123));
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testBindInetAddress() throws Exception {

        InetAddress inetAddress = InetAddress.getLoopbackAddress();

        sniffySocket.bind(inetAddress, 123);

        verify(delegate).bind(eq(inetAddress), eq(123));
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testListen() throws Exception {

        sniffySocket.listen(123);

        verify(delegate).listen(eq(123));
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testAccept() throws Exception {

        SocketImpl socketImpl = new SnifferSocketImpl(null);

        sniffySocket.accept(socketImpl);

        verify(delegate).accept(eq(socketImpl));
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testGetInputStream() throws Exception {

        InputStream expected = new ByteArrayInputStream(new byte[]{1, 2, 3});

        when(delegate.getInputStream()).thenReturn(expected);

        InputStream actual = sniffySocket.getInputStream();

        verify(delegate).getInputStream();
        verifyNoMoreInteractions(delegate);

        assertEquals(SnifferInputStream.class, actual.getClass());
        assertEquals(1, actual.read());

    }

    @Test
    public void testGetOutputStream() throws Exception {

        ByteArrayOutputStream expected = new ByteArrayOutputStream();

        when(delegate.getOutputStream()).thenReturn(expected);

        OutputStream actual = sniffySocket.getOutputStream();

        verify(delegate).getOutputStream();
        verifyNoMoreInteractions(delegate);

        assertEquals(SnifferOutputStream.class, actual.getClass());
        actual.write(1);

        assertArrayEquals(new byte[]{1}, expected.toByteArray());

    }

    @Test
    public void testAvailable() throws Exception {

        int expected = 1;

        when(delegate.available()).thenReturn(expected);

        int actual = sniffySocket.available();

        verify(delegate).available();
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testClose() throws Exception {

        sniffySocket.close();

        verify(delegate).close();
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testSetOption() throws Exception {

        int optId = 1;
        Object option = new Object();

        sniffySocket.setOption(optId, option);

        verify(delegate).setOption(eq(optId), eq(option));
        verifyNoMoreInteractions(delegate);

    }


    @Test
    public void testGetOption() throws Exception {

        Object expected = new Object();

        when(delegate.getOption(eq(1))).thenReturn(expected);

        Object actual = sniffySocket.getOption(1);

        verify(delegate).getOption(eq(1));
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testToString() throws Exception {

        String expected = "expected";

        when(delegate.toString()).thenReturn(expected);

        String actual = sniffySocket.toString();

        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Issue("issues/317")
    @Test
    public void testSetConnectionStatus() throws Exception {

        sniffySocket.connect("google.com", 443);

        sniffySocket.setConnectionStatus(-1);

        try {
            sniffySocket.connect("google.com", 443);
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

    }


    @Issue("issues/317")
    @Test
    public void testCheckConnectionAllowed() throws Exception {

        sniffySocket.connect("google.com", 443);

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("google.com", 443, -1);

        try {
            sniffySocket.connect("google.com", 443);
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

    }

}
