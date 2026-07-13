package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.SpyConfiguration;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SniffySSLNetworkConnection;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/** Publishes raw/decrypted traffic and owns the one-shot TLS ClientHello correlation state. */
final class SocketChannelTrafficPublisher {

    private static final Polyglog LOG = PolyglogFactory.log(SocketChannelTrafficPublisher.class);

    private final SniffySocketChannel connection;
    private final int connectionId;
    private boolean firstOutboundChunk = true;

    SocketChannelTrafficPublisher(SniffySocketChannel connection, int connectionId) {
        this.connection = connection;
        this.connectionId = connectionId;
    }

    void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len,
                    boolean connectHandshake) {
        SpyConfiguration configuration = Sniffy.getEffectiveSpyConfiguration();
        if (!configuration.isCaptureNetworkTraffic()) return;

        LOG.trace("Publishing raw channel traffic; sent = " + sent + "; len = " + len
                + "; connectionId = " + connectionId);
        Sniffy.logTraffic(
                connectionId, connection.getInetSocketAddress(), sent, protocol,
                traffic, off, len, configuration.isCaptureStackTraces());
        if (!connectHandshake) {
            correlateFirstOutboundChunk(sent, traffic, off, len);
        }
    }

    void logDecryptedTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        SpyConfiguration configuration = Sniffy.getEffectiveSpyConfiguration();
        if (!configuration.isCaptureNetworkTraffic()) return;

        LOG.trace("Publishing decrypted channel traffic; sent = " + sent + "; len = " + len
                + "; connectionId = " + connectionId);
        InetSocketAddress proxiedAddress = connection.getProxiedInetSocketAddress();
        Sniffy.logDecryptedTraffic(
                connectionId,
                proxiedAddress == null ? connection.getInetSocketAddress() : proxiedAddress,
                sent, protocol, traffic, off, len, configuration.isCaptureStackTraces());
    }

    private void correlateFirstOutboundChunk(boolean sent, byte[] traffic, int off, int len) {
        if (sent && firstOutboundChunk) {
            try {
                SniffySSLNetworkConnection sslConnection =
                        Sniffy.CLIENT_HELLO_CACHE.get(ByteBuffer.wrap(traffic, off, len));
                if (sslConnection != null) {
                    sslConnection.setSniffyNetworkConnection(connection);
                }
            } finally {
                // A captured non-CONNECT outbound chunk consumes the single correlation attempt,
                // including cache misses and callback failures.
                firstOutboundChunk = false;
            }
        }
    }

}
