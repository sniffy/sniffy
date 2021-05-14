package io.sniffy.tls;

import io.sniffy.Sniffy;
import io.sniffy.SpyConfiguration;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SnifferInputStream;
import io.sniffy.socket.SnifferOutputStream;
import io.sniffy.socket.TrafficCapturingNetworkConnection;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public class SniffySSLSocket extends SSLSocketAdapter implements TrafficCapturingNetworkConnection {

    // TODO: cover all methods with unit tests

    private static final Polyglog LOG = PolyglogFactory.log(SniffySSLSocket.class);

    private final SocketChannel socketChannel; // TODO: support

    private InetSocketAddress address;

    private final int id = Sniffy.CONNECTION_ID_SEQUENCE.getAndIncrement(); // TODO: reuse from SniffySocket if possible

    @Override
    public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration effectiveSpyConfiguration = Sniffy.getEffectiveSpyConfiguration();
        if (effectiveSpyConfiguration.isCaptureNetworkTraffic()) {
            LOG.trace("SniffySSLSocket.logTraffic() called; sent = " + sent + "; len = " + len + "; connectionId = " + id);
            Sniffy.logDecryptedTraffic(
                    id, address,
                    sent, protocol,
                    traffic, off, len,
                    effectiveSpyConfiguration.isCaptureStackTraces()
            );
        }
    }
    @Override
    public void logDecryptedTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        logTraffic(sent, protocol, traffic, off, len);
    }

    //

    public SniffySSLSocket(Socket delegate, InetSocketAddress address) {
        this((SSLSocket) delegate, address); // TODO: support when we cannot cast
    }

    public SniffySSLSocket(SSLSocket delegate, InetSocketAddress address) {
        super(delegate);
        LOG.trace("Created SniffySSLSocket for delegate " + delegate + " and address " + address + "; id = " + id);
        this.socketChannel = null;
        if (null == address) {
            this.address = (InetSocketAddress) delegate.getRemoteSocketAddress();
        } else {
            this.address = address;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new SnifferInputStream(this, super.getInputStream());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new SnifferOutputStream(this, super.getOutputStream());
    }

    // TODO: override other methods like sendUrgentData, connect, etc.

}
