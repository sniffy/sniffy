package io.sniffy.socket;

import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

// TODO: support HTTPS proxies as well (HTTPS tunnel over HTTPS) - low priority since rarely used
/**
 * @since 3.1.13
 */
public class SniffyPacketAnalyzer {

    /**
     * @since 3.1.13
     */
    private final SniffyNetworkConnection snifferSocket;

    /**
     * @since 3.1.13
     */
    public SniffyPacketAnalyzer(SniffyNetworkConnection snifferSocket) {
        this.snifferSocket = snifferSocket;
    }

    /**
     * @since 3.1.13
     */
    public void analyze(byte[] b, int off, int len) throws Exception {

        if (!Boolean.TRUE.equals(SniffyConfiguration.INSTANCE.getInterceptProxyConnections())) return;

        InetSocketAddress proxiedInetSocketAddress = null;

        @SuppressWarnings("CharsetObjectCanBeUsed") String potentialRequest = new String(b, off, len, Charset.forName("US-ASCII"));

        // TODO: support CONNECT header sent in multiple small chunks
        // TODO: support SOCKS proxy connection
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

        // TODO: set flag to allow capturing proxied traffic
        // TODO: think if we can capture decrypted traffic sent over proxied connection

    }

}
