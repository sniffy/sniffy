package io.sniffy.socket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.sniffy.socket.SocketsRegistry.SocketAddressStatus.OPEN;

public enum SocketsRegistry {
    INSTANCE;

    public enum SocketAddressStatus {
        OPEN,
        CLOSED
    }

    private Map<Map.Entry<String,Integer>, SocketAddressStatus> discoveredAdresses = new
            ConcurrentHashMap<Map.Entry<String,Integer>, SocketAddressStatus>();

    public SocketAddressStatus resolveSocketAddressStatus(InetSocketAddress inetSocketAddress) {

        InetAddress inetAddress = inetSocketAddress.getAddress();

        for (Map.Entry<Map.Entry<String,Integer>, SocketAddressStatus> entry : discoveredAdresses.entrySet()) {

            String hostName = entry.getKey().getKey();
            Integer port = entry.getKey().getValue();

            if ((null == hostName || hostName.equals(inetAddress.getHostName()) || hostName.equals(inetAddress.getHostAddress()) || hostName.equals(inetAddress.getCanonicalHostName())) &&
                    (null == port || port == inetSocketAddress.getPort()) &&
                    OPEN != entry.getValue()) {
                return entry.getValue();
            }

        }

        setSocketAddressStatus(inetSocketAddress.getHostName(), inetSocketAddress.getPort(), OPEN);

        return OPEN;

    }

    public Map<Map.Entry<String, Integer>, SocketAddressStatus> getDiscoveredAdresses() {
        return discoveredAdresses;
    }

    public void setSocketAddressStatus(String socketAddress, SocketAddressStatus socketAddressStatus) {

        String hostName = null;
        Integer port = null;

        if (null != socketAddress) {
            if (-1 != socketAddress.indexOf(':')) {
                String[] split = socketAddress.split(":");
                hostName = split[0];
                port = Integer.valueOf(split[1]);
            } else {
                hostName = socketAddress;
            }
        }

        setSocketAddressStatus(hostName, port, socketAddressStatus);

    }

    public void setSocketAddressStatus(String hostName, Integer port, SocketAddressStatus socketAddressStatus) {
        discoveredAdresses.put(new AbstractMap.SimpleEntry<String, Integer>(hostName, port), socketAddressStatus);
    }

    public void clear() {
        discoveredAdresses.clear();
    }

}
