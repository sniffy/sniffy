package io.sniffy.socket;

import io.sniffy.util.SocketUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.sniffy.socket.SocketsRegistry.ConnectionStatus.OPEN;

public enum SocketsRegistry {
    INSTANCE;

    public enum ConnectionStatus {
        OPEN,
        CLOSED
    }

    private Map<Map.Entry<String,Integer>, ConnectionStatus> discoveredAdresses = new
            ConcurrentHashMap<Map.Entry<String,Integer>, ConnectionStatus>();

    private Map<Map.Entry<String,String>, ConnectionStatus> discoveredDataSources = new
            ConcurrentHashMap<Map.Entry<String,String>, ConnectionStatus>();

    public ConnectionStatus resolveDataSourceStatus(String url, String userName) {

        for (Map.Entry<Map.Entry<String, String>, ConnectionStatus> entry : discoveredDataSources.entrySet()) {

            if ((null == url || url.equals(entry.getKey().getKey())) &&
                    (null == userName || userName.equals(entry.getKey().getValue())) &&
                    OPEN != entry.getValue()) {
                return entry.getValue();
            }

            setDataSourceStatus(url, userName, OPEN);

        }

        return OPEN;

    }

    public ConnectionStatus resolveSocketAddressStatus(InetSocketAddress inetSocketAddress) {

        InetAddress inetAddress = inetSocketAddress.getAddress();

        for (Map.Entry<Map.Entry<String,Integer>, ConnectionStatus> entry : discoveredAdresses.entrySet()) {

            String hostName = entry.getKey().getKey();
            Integer port = entry.getKey().getValue();

            if ((null == hostName || hostName.equals(inetAddress.getHostName()) || hostName.equals(inetAddress.getHostAddress())) &&
                    (null == port || port == inetSocketAddress.getPort()) &&
                    OPEN != entry.getValue()) {
                return entry.getValue();
            }

        }

        setSocketAddressStatus(inetSocketAddress.getHostName(), inetSocketAddress.getPort(), OPEN);

        return OPEN;

    }

    public Map<Map.Entry<String, Integer>, ConnectionStatus> getDiscoveredAddresses() {
        return discoveredAdresses;
    }

    public void setSocketAddressStatus(String socketAddress, ConnectionStatus connectionStatus) {

        Map.Entry<String, Integer> hostAndPort = SocketUtil.parseSocketAddress(socketAddress);

        String hostName = hostAndPort.getKey();
        Integer port = hostAndPort.getValue();

        setSocketAddressStatus(hostName, port, connectionStatus);

    }

    public void setSocketAddressStatus(String hostName, Integer port, ConnectionStatus connectionStatus) {
        discoveredAdresses.put(new AbstractMap.SimpleEntry<String, Integer>(hostName, port), connectionStatus);
    }

    public Map<Map.Entry<String, String>, ConnectionStatus> getDiscoveredDataSources() {
        return discoveredDataSources;
    }

    public void setDataSourceStatus(String url, String userName, ConnectionStatus status) {
        discoveredDataSources.put(new AbstractMap.SimpleEntry<String, String>(userName, url), status);
    }

    public void clear() {
        discoveredAdresses.clear();
        discoveredDataSources.clear();
    }

}
