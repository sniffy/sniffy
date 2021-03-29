package io.sniffy.tls;

import io.sniffy.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SSLSocketAdapterMockitoTest {

    @Mock
    private SSLSocket delegate;

    private SSLSocketAdapter sslSocketAdapter;

    @Before
    public void createSslSocketAdapter() {
        sslSocketAdapter = new SSLSocketAdapter(delegate);
    }

    @Test
    public void testGetSupportedCipherSuites() {
        String[] supportCipherSuites = new String[]{"CIPHER-1", "CIPHER-2"};
        when(delegate.getSupportedCipherSuites()).thenReturn(supportCipherSuites);
        assertArrayEquals(supportCipherSuites, sslSocketAdapter.getSupportedCipherSuites());
    }

    @Test
    public void testGetEnabledCipherSuites() {
        String[] enabledCipherSuites = new String[]{"CIPHER-1"};
        when(delegate.getEnabledCipherSuites()).thenReturn(enabledCipherSuites);
        assertArrayEquals(enabledCipherSuites, sslSocketAdapter.getEnabledCipherSuites());
    }

    @Test
    public void testSetEnabledCipherSuites() {
        String[] enabledCipherSuites = new String[]{"CIPHER-1"};
        ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
        sslSocketAdapter.setEnabledCipherSuites(enabledCipherSuites);
        verify(delegate).setEnabledCipherSuites(argumentCaptor.capture());
        assertArrayEquals(enabledCipherSuites, argumentCaptor.getValue());
    }

    @Test
    public void testGetSupportedProtocols() {
        String[] supportProtocols = new String[]{"PROTOCOL-1", "PROTOCOL-2"};
        when(delegate.getSupportedProtocols()).thenReturn(supportProtocols);
        assertArrayEquals(supportProtocols, sslSocketAdapter.getSupportedProtocols());
    }

    @Test
    public void testGetEnabledProtocols() {
        String[] enabledProtocols = new String[]{"PROTOCOL-1"};
        when(delegate.getEnabledProtocols()).thenReturn(enabledProtocols);
        assertArrayEquals(enabledProtocols, sslSocketAdapter.getEnabledProtocols());
    }

    @Test
    public void testSetEnabledProtocols() {
        String[] enabledProtocols = new String[]{"PROTOCOL-1"};
        ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
        sslSocketAdapter.setEnabledProtocols(enabledProtocols);
        verify(delegate).setEnabledProtocols(argumentCaptor.capture());
        assertArrayEquals(enabledProtocols, argumentCaptor.getValue());
    }

    @Test
    public void testGetSession() {
        SSLSession sslSession = mock(SSLSession.class);
        when(delegate.getSession()).thenReturn(sslSession);
        assertEquals(sslSession, sslSocketAdapter.getSession());
    }

    @Test
    public void testGetHandshakeSession() {
        SSLSession sslSession = mock(SSLSession.class);
        when(delegate.getHandshakeSession()).thenReturn(sslSession);
        assertEquals(sslSession, sslSocketAdapter.getHandshakeSession());
    }

    @Test
    public void testAddHandshakeCompletedListener() {
        HandshakeCompletedListener handshakeCompletedListener = mock(HandshakeCompletedListener.class);
        ArgumentCaptor<HandshakeCompletedListener> argumentCaptor = ArgumentCaptor.forClass(HandshakeCompletedListener.class);
        sslSocketAdapter.addHandshakeCompletedListener(handshakeCompletedListener);
        verify(delegate).addHandshakeCompletedListener(argumentCaptor.capture());
        assertEquals(handshakeCompletedListener, argumentCaptor.getValue());
    }

    @Test
    public void testRemoveHandshakeCompletedListener() {
        HandshakeCompletedListener handshakeCompletedListener = mock(HandshakeCompletedListener.class);
        ArgumentCaptor<HandshakeCompletedListener> argumentCaptor = ArgumentCaptor.forClass(HandshakeCompletedListener.class);
        sslSocketAdapter.removeHandshakeCompletedListener(handshakeCompletedListener);
        verify(delegate).removeHandshakeCompletedListener(argumentCaptor.capture());
        assertEquals(handshakeCompletedListener, argumentCaptor.getValue());
    }

    @Test
    public void testStartHandshake() throws IOException {
        sslSocketAdapter.startHandshake();
        verify(delegate).startHandshake();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void setUseClientMode() {
        ArgumentCaptor<Boolean> argumentCaptor = ArgumentCaptor.forClass(Boolean.TYPE);
        sslSocketAdapter.setUseClientMode(true);
        verify(delegate).setUseClientMode(argumentCaptor.capture());
        assertEquals(true, argumentCaptor.getValue());
    }

    @Test
    public void testGetUseClientMode() {
        when(delegate.getUseClientMode()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.getUseClientMode());
    }

    @Test
    public void testSetNeedClientAuth() {
        ArgumentCaptor<Boolean> argumentCaptor = ArgumentCaptor.forClass(Boolean.TYPE);
        sslSocketAdapter.setNeedClientAuth(true);
        verify(delegate).setNeedClientAuth(argumentCaptor.capture());
        assertEquals(true, argumentCaptor.getValue());
    }

    @Test
    public void testGetNeedClientAuth() {
        when(delegate.getNeedClientAuth()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.getNeedClientAuth());
    }

    @Test
    public void testSetWantClientAuth() {
        ArgumentCaptor<Boolean> argumentCaptor = ArgumentCaptor.forClass(Boolean.TYPE);
        sslSocketAdapter.setWantClientAuth(true);
        verify(delegate).setWantClientAuth(argumentCaptor.capture());
        assertEquals(true, argumentCaptor.getValue());
    }

    @Test
    public void testGetWantClientAuth() {
        when(delegate.getWantClientAuth()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.getWantClientAuth());
    }

    @Test
    public void testSetEnableSessionCreation() {
        ArgumentCaptor<Boolean> argumentCaptor = ArgumentCaptor.forClass(Boolean.TYPE);
        sslSocketAdapter.setEnableSessionCreation(true);
        verify(delegate).setEnableSessionCreation(argumentCaptor.capture());
        assertEquals(true, argumentCaptor.getValue());
    }

    @Test
    public void testGetEnableSessionCreation() {
        when(delegate.getEnableSessionCreation()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.getEnableSessionCreation());
    }

    @Test
    public void testGetSSLParameters() {
        SSLParameters sslParameters = mock(SSLParameters.class);
        when(delegate.getSSLParameters()).thenReturn(sslParameters);
        assertEquals(sslParameters, sslSocketAdapter.getSSLParameters());
    }

    @Test
    public void testSetSSLParameters() {
        SSLParameters sslParameters = Mockito.mock(SSLParameters.class);
        ArgumentCaptor<SSLParameters> argumentCaptor = ArgumentCaptor.forClass(SSLParameters.class);
        sslSocketAdapter.setSSLParameters(sslParameters);
        verify(delegate).setSSLParameters(argumentCaptor.capture());
        assertEquals(sslParameters, argumentCaptor.getValue());
    }

    // Following 4 methods are tested using ReflectionUtil since they are not available in early versions of JDK 1.8

    @Test
    public void testGetApplicationProtocol() throws Exception {
        when(ReflectionUtil.invokeMethod(SSLSocket.class, delegate, "getApplicationProtocol", String.class)).thenReturn("TLS");
        assertEquals("TLS", ReflectionUtil.invokeMethod(SSLSocket.class, sslSocketAdapter, "getApplicationProtocol", String.class));
    }

    @Test
    public void testGetHandshakeApplicationProtocol() throws Exception {
        when(ReflectionUtil.invokeMethod(SSLSocket.class, delegate, "getHandshakeApplicationProtocol", String.class)).thenReturn("TLS");
        assertEquals("TLS", ReflectionUtil.invokeMethod(SSLSocket.class, sslSocketAdapter, "getHandshakeApplicationProtocol", String.class));
    }

    @Mock
    private BiFunction<SSLSocket, List<String>, String> selectorMock;

    @Captor
    private ArgumentCaptor<BiFunction<SSLSocket, List<String>, String>> selectorCaptor;

    @Test
    public void testSetHandshakeApplicationProtocolSelector() throws Exception {
        ReflectionUtil.invokeMethod(SSLSocket.class, sslSocketAdapter, "setHandshakeApplicationProtocolSelector", BiFunction.class, selectorMock, Void.class);
        ReflectionUtil.invokeMethod(SSLSocket.class, verify(delegate), "setHandshakeApplicationProtocolSelector", BiFunction.class, selectorCaptor.capture(), Void.class);
        assertEquals(selectorMock, selectorCaptor.getValue());
    }

    @Test
    public void testGetHandshakeApplicationProtocolSelector() throws Exception {

        when(ReflectionUtil.invokeMethod(SSLSocket.class, delegate, "getHandshakeApplicationProtocolSelector", BiFunction.class)).thenReturn(selectorMock);
        //noinspection unchecked
        BiFunction<SSLSocket, List<String>, String> selector = (BiFunction<SSLSocket, List<String>, String>)
                ReflectionUtil.invokeMethod(SSLSocket.class, sslSocketAdapter, "getHandshakeApplicationProtocolSelector", BiFunction.class);
        assertEquals(selectorMock, selector);
    }

    @Test
    public void testConnect() throws IOException {
        SocketAddress socketAddress = Mockito.mock(SocketAddress.class);
        ArgumentCaptor<SocketAddress> argumentCaptor = ArgumentCaptor.forClass(SocketAddress.class);
        sslSocketAdapter.connect(socketAddress);
        verify(delegate).connect(argumentCaptor.capture());
        assertEquals(socketAddress, argumentCaptor.getValue());
    }

    @Test
    public void connect() throws IOException {
        SocketAddress socketAddress = Mockito.mock(SocketAddress.class);
        ArgumentCaptor<SocketAddress> argumentCaptor = ArgumentCaptor.forClass(SocketAddress.class);
        ArgumentCaptor<Integer> timeoutArgumentCaptor = ArgumentCaptor.forClass(Integer.TYPE);
        sslSocketAdapter.connect(socketAddress, 42);
        verify(delegate).connect(argumentCaptor.capture(), timeoutArgumentCaptor.capture());
        assertEquals(socketAddress, argumentCaptor.getValue());
        assertEquals(42, (int) timeoutArgumentCaptor.getValue());
    }

    @Test
    public void testBind() throws IOException {
        SocketAddress socketAddress = Mockito.mock(SocketAddress.class);
        ArgumentCaptor<SocketAddress> argumentCaptor = ArgumentCaptor.forClass(SocketAddress.class);
        sslSocketAdapter.bind(socketAddress);
        verify(delegate).bind(argumentCaptor.capture());
        assertEquals(socketAddress, argumentCaptor.getValue());
    }

    @Test
    public void testGetInetAddress() {
        InetAddress inetAddress = mock(InetAddress.class);
        when(delegate.getInetAddress()).thenReturn(inetAddress);
        assertEquals(inetAddress, sslSocketAdapter.getInetAddress());
    }

    @Test
    public void testGetLocalAddress() {
        InetAddress inetAddress = mock(InetAddress.class);
        when(delegate.getLocalAddress()).thenReturn(inetAddress);
        assertEquals(inetAddress, sslSocketAdapter.getLocalAddress());
    }

    @Test
    public void testGetPort() {
        when(delegate.getPort()).thenReturn(42);
        assertEquals(42, sslSocketAdapter.getPort());
    }

    @Test
    public void testGetLocalPort() {
        when(delegate.getLocalPort()).thenReturn(42);
        assertEquals(42, sslSocketAdapter.getLocalPort());
    }

    @Test
    public void testGetRemoteSocketAddress() {
        SocketAddress socketAddress = mock(SocketAddress.class);
        when(delegate.getRemoteSocketAddress()).thenReturn(socketAddress);
        assertEquals(socketAddress, sslSocketAdapter.getRemoteSocketAddress());
    }

    @Test
    public void testGetLocalSocketAddress() {
        SocketAddress socketAddress = mock(SocketAddress.class);
        when(delegate.getLocalSocketAddress()).thenReturn(socketAddress);
        assertEquals(socketAddress, sslSocketAdapter.getLocalSocketAddress());
    }

    @Test
    public void testGetChannel() {
        SocketChannel socketChannel = mock(SocketChannel.class);
        when(delegate.getChannel()).thenReturn(socketChannel);
        assertEquals(socketChannel, sslSocketAdapter.getChannel());
    }

    @Test
    public void testGetInputStream() throws IOException {
        InputStream inputStream = mock(InputStream.class);
        when(delegate.getInputStream()).thenReturn(inputStream);
        assertEquals(inputStream, sslSocketAdapter.getInputStream());
    }

    @Test
    public void testGetOutputStream() throws IOException {
        OutputStream outputStream = mock(OutputStream.class);
        when(delegate.getOutputStream()).thenReturn(outputStream);
        assertEquals(outputStream, sslSocketAdapter.getOutputStream());
    }

    @Test
    public void testSetTcpNoDelay() throws SocketException {
        ArgumentCaptor<Boolean> argumentCaptor = ArgumentCaptor.forClass(Boolean.TYPE);
        sslSocketAdapter.setTcpNoDelay(true);
        verify(delegate).setTcpNoDelay(argumentCaptor.capture());
        assertEquals(true, argumentCaptor.getValue());
    }

    @Test
    public void testGetTcpNoDelay() throws SocketException {
        when(delegate.getTcpNoDelay()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.getTcpNoDelay());
    }


    @Test
    public void testSetSoLinger() throws SocketException {
        ArgumentCaptor<Boolean> argument1Captor = ArgumentCaptor.forClass(Boolean.TYPE);
        ArgumentCaptor<Integer> argument2Captor = ArgumentCaptor.forClass(Integer.TYPE);
        sslSocketAdapter.setSoLinger(true, 42);
        verify(delegate).setSoLinger(argument1Captor.capture(), argument2Captor.capture());
        assertEquals(true, argument1Captor.getValue());
        assertEquals(42, (int) argument2Captor.getValue());
    }

    @Test
    public void testGetSoLinger() throws SocketException {
        when(delegate.getSoLinger()).thenReturn(42);
        assertEquals(42, sslSocketAdapter.getSoLinger());
    }

    @Test
    public void testSendUrgentData() throws IOException {
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.TYPE);
        sslSocketAdapter.sendUrgentData(42);
        verify(delegate).sendUrgentData(argumentCaptor.capture());
        assertEquals(42, (int) argumentCaptor.getValue());
    }

    @Test
    public void testSetOOBInline() throws SocketException {
        ArgumentCaptor<Boolean> argumentCaptor = ArgumentCaptor.forClass(Boolean.TYPE);
        sslSocketAdapter.setOOBInline(true);
        verify(delegate).setOOBInline(argumentCaptor.capture());
        assertEquals(true, argumentCaptor.getValue());
    }

    @Test
    public void testGetOOBInline() throws SocketException {
        when(delegate.getOOBInline()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.getOOBInline());
    }

    @Test
    public void testSetSoTimeout() throws SocketException {
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.TYPE);
        sslSocketAdapter.setSoTimeout(42);
        verify(delegate).setSoTimeout(argumentCaptor.capture());
        assertEquals(42, (int) argumentCaptor.getValue());
    }

    @Test
    public void testGetSoTimeout() throws SocketException {
        when(delegate.getSoTimeout()).thenReturn(42);
        assertEquals(42, sslSocketAdapter.getSoTimeout());
    }

    @Test
    public void testSetSendBufferSize() throws SocketException {
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.TYPE);
        sslSocketAdapter.setSendBufferSize(42);
        verify(delegate).setSendBufferSize(argumentCaptor.capture());
        assertEquals(42, (int) argumentCaptor.getValue());
    }

    @Test
    public void testGetSendBufferSize() throws SocketException {
        when(delegate.getSendBufferSize()).thenReturn(42);
        assertEquals(42, sslSocketAdapter.getSendBufferSize());
    }

    @Test
    public void testSetReceiveBufferSize() throws SocketException {
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.TYPE);
        sslSocketAdapter.setReceiveBufferSize(42);
        verify(delegate).setReceiveBufferSize(argumentCaptor.capture());
        assertEquals(42, (int) argumentCaptor.getValue());
    }

    @Test
    public void testGetReceiveBufferSize() throws SocketException {
        when(delegate.getReceiveBufferSize()).thenReturn(42);
        assertEquals(42, sslSocketAdapter.getReceiveBufferSize());
    }

    @Test
    public void testSetKeepAlive() throws SocketException {
        ArgumentCaptor<Boolean> argumentCaptor = ArgumentCaptor.forClass(Boolean.TYPE);
        sslSocketAdapter.setKeepAlive(true);
        verify(delegate).setKeepAlive(argumentCaptor.capture());
        assertEquals(true, argumentCaptor.getValue());
    }

    @Test
    public void testGetKeepAlive() throws SocketException {
        when(delegate.getKeepAlive()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.getKeepAlive());
    }

    @Test
    public void testSetTrafficClass() throws SocketException {
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.TYPE);
        sslSocketAdapter.setTrafficClass(42);
        verify(delegate).setTrafficClass(argumentCaptor.capture());
        assertEquals(42, (int) argumentCaptor.getValue());
    }

    @Test
    public void getTrafficClass() throws SocketException {
        when(delegate.getTrafficClass()).thenReturn(42);
        assertEquals(42, sslSocketAdapter.getTrafficClass());
    }

    @Test
    public void testSetReuseAddress() throws SocketException {
        ArgumentCaptor<Boolean> argumentCaptor = ArgumentCaptor.forClass(Boolean.TYPE);
        sslSocketAdapter.setReuseAddress(true);
        verify(delegate).setReuseAddress(argumentCaptor.capture());
        assertEquals(true, argumentCaptor.getValue());
    }

    @Test
    public void testGetReuseAddress() throws SocketException {
        when(delegate.getReuseAddress()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.getReuseAddress());
    }

    @Test
    public void testClose() throws IOException {
        sslSocketAdapter.close();
        verify(delegate).close();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testShutdownInput() throws IOException {
        sslSocketAdapter.shutdownInput();
        verify(delegate).shutdownInput();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testShutdownOutput() throws IOException {
        sslSocketAdapter.shutdownOutput();
        verify(delegate).shutdownOutput();
        verifyNoMoreInteractions(delegate);
    }

    // TODO: evaluate if we need to delegate it
    /*@Override
    public String toString() {
        return delegate.toString();
    }*/

    @Test
    public void testIsConnected() {
        when(delegate.isConnected()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.isConnected());
    }

    @Test
    public void testIsBound() {
        when(delegate.isBound()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.isBound());
    }

    @Test
    public void testIsClosed() {
        when(delegate.isClosed()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.isClosed());
    }

    @Test
    public void testIsInputShutdown() {
        when(delegate.isInputShutdown()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.isInputShutdown());
    }

    @Test
    public void testIsOutputShutdown() {
        when(delegate.isOutputShutdown()).thenReturn(true);
        //noinspection SimplifiableAssertion
        assertEquals(true, sslSocketAdapter.isOutputShutdown());
    }

    @Test
    public void testSetPerformancePreferences() {
        ArgumentCaptor<Integer> argument1Captor = ArgumentCaptor.forClass(Integer.TYPE);
        ArgumentCaptor<Integer> argument2Captor = ArgumentCaptor.forClass(Integer.TYPE);
        ArgumentCaptor<Integer> argument3Captor = ArgumentCaptor.forClass(Integer.TYPE);
        sslSocketAdapter.setPerformancePreferences(1, 2, 3);
        verify(delegate).setPerformancePreferences(argument1Captor.capture(), argument2Captor.capture(), argument3Captor.capture());
        assertEquals(1, (int) argument1Captor.getValue());
        assertEquals(2, (int) argument2Captor.getValue());
        assertEquals(3, (int) argument3Captor.getValue());
    }

}
