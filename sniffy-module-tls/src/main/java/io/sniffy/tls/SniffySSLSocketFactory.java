package io.sniffy.tls;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SniffySSLSocketFactory extends SSLSocketFactory {

    // TODO: support SSLSocketFactory.theFactory

    private final SSLSocketFactory delegate;

    public SniffySSLSocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return new SniffySSLSocket(delegate.createSocket(s, host, port, autoClose), InetSocketAddress.createUnresolved(host, port));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return new SniffySSLSocket(delegate.createSocket(host, port), InetSocketAddress.createUnresolved(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return new SniffySSLSocket(delegate.createSocket(host, port, localHost, localPort), InetSocketAddress.createUnresolved(host, port));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return new SniffySSLSocket(delegate.createSocket(host, port), new InetSocketAddress(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return new SniffySSLSocket(delegate.createSocket(address, port, localAddress, localPort), new InetSocketAddress(address, port));
    }

}
