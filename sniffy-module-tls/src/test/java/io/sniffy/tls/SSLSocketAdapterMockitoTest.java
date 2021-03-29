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
import java.net.InetAddress;
import java.net.SocketAddress;
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

/*
    @Override
    public SocketAddress getRemoteSocketAddress() {
        return delegate.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return delegate.getLocalSocketAddress();
    }

    @Override
    public SocketChannel getChannel() {
        return delegate.getChannel();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        delegate.setTcpNoDelay(on);
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return delegate.getTcpNoDelay();
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        delegate.setSoLinger(on, linger);
    }

    @Override
    public int getSoLinger() throws SocketException {
        return delegate.getSoLinger();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        delegate.sendUrgentData(data);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        delegate.setOOBInline(on);
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return delegate.getOOBInline();
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        delegate.setSoTimeout(timeout);
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return delegate.getSoTimeout();
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
        delegate.setSendBufferSize(size);
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return delegate.getSendBufferSize();
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        delegate.setReceiveBufferSize(size);
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return delegate.getReceiveBufferSize();
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        delegate.setKeepAlive(on);
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return delegate.getKeepAlive();
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        delegate.setTrafficClass(tc);
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return delegate.getTrafficClass();
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        delegate.setReuseAddress(on);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return delegate.getReuseAddress();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void shutdownInput() throws IOException {
        delegate.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        delegate.shutdownOutput();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public boolean isInputShutdown() {
        return delegate.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return delegate.isOutputShutdown();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
    }*/

}
