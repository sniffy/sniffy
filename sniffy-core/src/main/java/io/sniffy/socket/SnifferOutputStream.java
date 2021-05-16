package io.sniffy.socket;

import io.sniffy.registry.ConnectionsRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketOptions;
import java.nio.charset.Charset;

/**
 * @since 3.1
 */
public class SnifferOutputStream extends OutputStream {

    // TODO: refactor
    private final SniffyNetworkConnection snifferSocket;
    private final TrafficCapturingNetworkConnection trafficCapturingNetworkConnection;
    private final OutputStream delegate;

    public SnifferOutputStream(TrafficCapturingNetworkConnection trafficCapturingNetworkConnection, OutputStream delegate) {
        this.trafficCapturingNetworkConnection = trafficCapturingNetworkConnection;
        if (trafficCapturingNetworkConnection instanceof SniffyNetworkConnection) {
            this.snifferSocket = (SniffyNetworkConnection) trafficCapturingNetworkConnection;
        } else {
            this.snifferSocket = null;
        }
        this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
            trafficCapturingNetworkConnection.logTraffic(
                    true, Protocol.TCP,
                    new byte[]{(byte) b},
                    0, 1
            );
        } finally {
            sleepIfRequired(1);
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start, 0, 1);
        }
    }

    /**
     * Adds a delay as defined for current {@link SnifferSocketImpl} in {@link ConnectionsRegistry#discoveredDataSources}
     * <p>
     * Delay is added for each <b>N</b> bytes sent where <b>N</b> is the value of {@link SniffyNetworkConnection#DEFAULT_TCP_WINDOW_SIZE}
     * <p>
     * If application writes <b>M</b> bytes where (k-1) * N &lt; M  &lt; k * N exactly <b>k</b> delays will be added
     * <p>
     * A call to {@link SnifferInputStream} obtained from the same {@link SnifferSocketImpl} and made from the same thread
     * will reset the number of buffered (i.e. which can be written without delay) bytes to 0 effectively adding a guaranteed
     * delay to any subsequent {@link SnifferOutputStream#write(int)} request
     *
     * @param bytesUp number of bytes sent to socket
     * @throws ConnectException on underlying socket exception
     */
    @SuppressWarnings("JavadocReference")
    private void sleepIfRequired(int bytesUp) throws ConnectException {

        if (null == snifferSocket) return;

        snifferSocket.setLastWriteThreadId(Thread.currentThread().getId());

        if (snifferSocket.getLastReadThreadId() == snifferSocket.getLastWriteThreadId()) {
            snifferSocket.setPotentiallyBufferedInputBytes(0);
        }

        int potentiallyBufferedOutputBytes = snifferSocket.getPotentiallyBufferedOutputBytes() - bytesUp;
        snifferSocket.setPotentiallyBufferedOutputBytes(potentiallyBufferedOutputBytes);

        if (potentiallyBufferedOutputBytes < 0) {
            int estimatedNumberOfTcpPackets = 1 + (-1 * potentiallyBufferedOutputBytes) / SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
            snifferSocket.checkConnectionAllowed(estimatedNumberOfTcpPackets);
            snifferSocket.setPotentiallyBufferedOutputBytes(SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE);
        }

    }

    @Override
    public void write(byte[] b) throws IOException {

        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);

        if (null != snifferSocket && !snifferSocket.isFirstPacketSent()) {

            InetSocketAddress proxiedInetSocketAddress = null;

            try {
                @SuppressWarnings("CharsetObjectCanBeUsed") String potentialRequest = new String(b, Charset.forName("US-ASCII"));

                // TODO: support CONNECT header sent in multiple small chunks
                int connectIx = potentialRequest.indexOf("CONNECT ");

                if (0 == connectIx) {
                    int crIx = potentialRequest.indexOf("\r");
                    int lfIx = potentialRequest.indexOf("\n");

                    int newLineIx;

                    if (crIx > 0) {
                        if (lfIx > 0) {
                            newLineIx = Math.min(crIx, lfIx);
                        } else {
                            newLineIx = lfIx;
                        }
                    } else {
                        if (lfIx > 0) {
                            newLineIx = lfIx;
                        } else {
                            newLineIx = -1;
                        }
                    }

                    if (newLineIx > 0) {
                        int httpVersionIx = potentialRequest.substring(0, newLineIx).indexOf(" HTTP/");
                        if (httpVersionIx > 0) {

                            String proxiedHostAndPort = potentialRequest.substring("CONNECT ".length(), httpVersionIx);

                            String host;
                            int port;

                            if (proxiedHostAndPort.contains(":")) {
                                host = proxiedHostAndPort.substring(0, proxiedHostAndPort.lastIndexOf(":"));
                                port = Integer.parseInt(proxiedHostAndPort.substring(proxiedHostAndPort.lastIndexOf(":") + 1));
                            } else {
                                host = proxiedHostAndPort;
                                port = 80;
                            }

                            proxiedInetSocketAddress = new InetSocketAddress(host, port);

                        }
                    }
                }

                if (null != proxiedInetSocketAddress) {
                    snifferSocket.setProxiedInetSocketAddress(proxiedInetSocketAddress);
                    ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(proxiedInetSocketAddress, snifferSocket);
                }

                // TODO: only capture traffic

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                snifferSocket.setFirstPacketSent(true);
            }

        }

        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
            // TODO: do the same with proxied connection id after CONNECT handshake is finished
            trafficCapturingNetworkConnection.logTraffic(
                    true, Protocol.TCP,
                    b,
                    0, b.length
            );
        } finally {
            sleepIfRequired(b.length);
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start, 0, b.length);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);

        if (null != snifferSocket && !snifferSocket.isFirstPacketSent()) {

            InetSocketAddress proxiedInetSocketAddress = null;

            try {
                @SuppressWarnings("CharsetObjectCanBeUsed") String potentialRequest = new String(b, off, len, Charset.forName("US-ASCII"));

                // TODO: support CONNECT header sent in multiple small chunks
                int connectIx = potentialRequest.indexOf("CONNECT ");

                if (0 == connectIx) {
                    int crIx = potentialRequest.indexOf("\r");
                    int lfIx = potentialRequest.indexOf("\n");

                    int newLineIx;

                    if (crIx > 0) {
                        if (lfIx > 0) {
                            newLineIx = Math.min(crIx, lfIx);
                        } else {
                            newLineIx = lfIx;
                        }
                    } else {
                        if (lfIx > 0) {
                            newLineIx = lfIx;
                        } else {
                            newLineIx = -1;
                        }
                    }

                    if (newLineIx > 0) {
                        int httpVersionIx = potentialRequest.substring(0, newLineIx).indexOf(" HTTP/");
                        if (httpVersionIx > 0) {

                            String proxiedHostAndPort = potentialRequest.substring("CONNECT ".length(), httpVersionIx);

                            String host;
                            int port;

                            if (proxiedHostAndPort.contains(":")) {
                                host = proxiedHostAndPort.substring(0, proxiedHostAndPort.lastIndexOf(":"));
                                port = Integer.parseInt(proxiedHostAndPort.substring(proxiedHostAndPort.lastIndexOf(":") + 1));
                            } else {
                                host = proxiedHostAndPort;
                                port = 80;
                            }

                            proxiedInetSocketAddress = new InetSocketAddress(host, port);

                        }
                    }
                }

                if (null != proxiedInetSocketAddress) {
                    snifferSocket.setProxiedInetSocketAddress(proxiedInetSocketAddress);
                    ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(proxiedInetSocketAddress, snifferSocket);
                }

                // TODO: only capture traffic

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                snifferSocket.setFirstPacketSent(true);
            }

        }

        long start = System.currentTimeMillis();
        try {
            delegate.write(b, off, len);
            trafficCapturingNetworkConnection.logTraffic(
                    true, Protocol.TCP,
                    b,
                    off, len
            );
        } finally {
            sleepIfRequired(len);
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start, 0, len);
        }
    }

    @Override
    public void flush() throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(1);
        long start = System.currentTimeMillis();
        try {
            delegate.flush();
        } finally {
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void close() throws IOException {
        if (null != snifferSocket) snifferSocket.checkConnectionAllowed(0);
        long start = System.currentTimeMillis();
        try {
            delegate.close();
        } finally {
            if (null != snifferSocket) snifferSocket.logSocket(System.currentTimeMillis() - start);
        }
    }

}
