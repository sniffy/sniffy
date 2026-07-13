package io.sniffy.nio;

import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SniffyPacketAnalyzer;

import java.io.ByteArrayOutputStream;

/**
 * Bounded post-write HTTP CONNECT detector and raw outbound capture state.
 * The owning channel calls all stateful methods while holding its
 * {@code connectionWriteLock}, preserving physical write/parser/publication order.
 */
final class SocketChannelOutboundTraffic {

    static final int INITIAL_PACKET_CAPTURE_LIMIT = 8192;

    private static final Polyglog LOG = PolyglogFactory.log(SocketChannelOutboundTraffic.class);

    private final SniffySocketChannel connection;
    private final SocketChannelTrafficPublisher trafficPublisher;
    private final ByteArrayOutputStream initialOutboundBytes = new ByteArrayOutputStream();

    private volatile boolean firstPacketSent;

    SocketChannelOutboundTraffic(SniffySocketChannel connection,
                                 SocketChannelTrafficPublisher trafficPublisher) {
        this.connection = connection;
        this.trafficPublisher = trafficPublisher;
    }

    void process(byte[] bytes, boolean captureNetworkTraffic) {
        if (bytes.length == 0) return;

        if (firstPacketSent) {
            publish(bytes, 0, bytes.length, captureNetworkTraffic, false);
            return;
        }

        if (!Boolean.TRUE.equals(SniffyConfiguration.INSTANCE.getInterceptProxyConnections())) {
            firstPacketSent = true;
            publish(bytes, 0, bytes.length, captureNetworkTraffic, false);
            return;
        }

        int available = INITIAL_PACKET_CAPTURE_LIMIT - initialOutboundBytes.size();
        int appended = Math.min(available, bytes.length);
        initialOutboundBytes.write(bytes, 0, appended);
        byte[] candidate = initialOutboundBytes.toByteArray();

        if (!isProxyDecisionComplete(candidate) && appended == bytes.length) {
            return;
        }

        try {
            new SniffyPacketAnalyzer(connection).analyze(candidate, 0, candidate.length);
        } catch (Exception e) {
            LOG.error(e);
        } finally {
            firstPacketSent = true;
        }

        if (captureNetworkTraffic) {
            int handshakeLength = connection.getProxiedInetSocketAddress() == null
                    ? 0 : httpHeaderLength(candidate);
            publish(candidate, 0, handshakeLength, true, true);
            publish(candidate, handshakeLength, candidate.length - handshakeLength, true, false);
            publish(bytes, appended, bytes.length - appended, true, false);
        }
        initialOutboundBytes.reset();
    }

    void finish(boolean captureNetworkTraffic, Runnable beforeFinalTrafficPublication) {
        if (firstPacketSent || initialOutboundBytes.size() == 0) return;

        byte[] pending = initialOutboundBytes.toByteArray();
        try {
            new SniffyPacketAnalyzer(connection).analyze(pending, 0, pending.length);
        } catch (Exception e) {
            LOG.error(e);
        } finally {
            firstPacketSent = true;
            initialOutboundBytes.reset();
        }
        if (captureNetworkTraffic) {
            beforeFinalTrafficPublication.run();
            trafficPublisher.logTraffic(true, Protocol.TCP, pending, 0, pending.length, false);
        }
    }

    boolean isFirstPacketSent() {
        return firstPacketSent;
    }

    void setFirstPacketSent(boolean firstPacketSent) {
        this.firstPacketSent = firstPacketSent;
    }

    int pendingByteCount() {
        return initialOutboundBytes.size();
    }

    private void publish(byte[] bytes, int offset, int length, boolean captureNetworkTraffic,
                         boolean connectHandshake) {
        if (captureNetworkTraffic && length > 0) {
            trafficPublisher.logTraffic(
                    true, Protocol.TCP, bytes, offset, length, connectHandshake);
        }
    }

    private static boolean isProxyDecisionComplete(byte[] candidate) {
        byte[] connectPrefix = new byte[]{'C', 'O', 'N', 'N', 'E', 'C', 'T', ' '};
        int prefixLength = Math.min(candidate.length, connectPrefix.length);
        for (int i = 0; i < prefixLength; i++) {
            if (candidate[i] != connectPrefix[i]) return true;
        }
        if (candidate.length < connectPrefix.length) return false;
        if (httpHeaderLength(candidate) > 0) return true;
        return candidate.length >= INITIAL_PACKET_CAPTURE_LIMIT;
    }

    private static int httpHeaderLength(byte[] bytes) {
        for (int i = 1; i < bytes.length; i++) {
            if ('\n' == bytes[i] && '\n' == bytes[i - 1]) return i + 1;
            if (i >= 3 && '\r' == bytes[i - 3] && '\n' == bytes[i - 2]
                    && '\r' == bytes[i - 1] && '\n' == bytes[i]) {
                return i + 1;
            }
        }
        return 0;
    }

}
