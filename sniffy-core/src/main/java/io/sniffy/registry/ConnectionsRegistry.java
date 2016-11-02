package io.sniffy.registry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.sniffy.registry.ConnectionsRegistry.ConnectionStatus.OPEN;

public enum ConnectionsRegistry {
    INSTANCE;

    public enum ConnectionStatus {
        OPEN,
        CLOSED
    }

    private Map<Map.Entry<String,Integer>, ConnectionStatus> discoveredAdresses = new
            ConcurrentHashMap<Map.Entry<String,Integer>, ConnectionStatus>();

    private Map<Map.Entry<String,String>, ConnectionStatus> discoveredDataSources = new
            ConcurrentHashMap<Map.Entry<String,String>, ConnectionStatus>();

    private boolean persistRegistry = false;

    public ConnectionStatus resolveDataSourceStatus(String url, String userName) {

        for (Map.Entry<Map.Entry<String, String>, ConnectionStatus> entry : discoveredDataSources.entrySet()) {

            if ((null == url || url.equals(entry.getKey().getKey())) &&
                    (null == userName || userName.equals(entry.getKey().getValue())) &&
                    OPEN != entry.getValue()) { // TODO: why OPEN !=  ???
                return entry.getValue();
            }

        }

        setDataSourceStatus(url, userName, OPEN);

        return OPEN;

    }

    public ConnectionStatus resolveSocketAddressStatus(InetSocketAddress inetSocketAddress) {

        InetAddress inetAddress = inetSocketAddress.getAddress();

        for (Map.Entry<Map.Entry<String,Integer>, ConnectionStatus> entry : discoveredAdresses.entrySet()) {

            String hostName = entry.getKey().getKey();
            Integer port = entry.getKey().getValue();

            if ((null == hostName || hostName.equals(inetAddress.getHostName()) || hostName.equals(inetAddress.getHostAddress())) &&
                    (null == port || port == inetSocketAddress.getPort()) &&
                    OPEN != entry.getValue()) { // TODO: why OPEN !=  ???
                return entry.getValue();
            }

        }

        setSocketAddressStatus(inetSocketAddress.getHostName(), inetSocketAddress.getPort(), OPEN);

        return OPEN;

    }

    public Map<Map.Entry<String, Integer>, ConnectionStatus> getDiscoveredAddresses() {
        return discoveredAdresses;
    }

    public void setSocketAddressStatus(String hostName, Integer port, ConnectionStatus connectionStatus) {
        discoveredAdresses.put(new AbstractMap.SimpleEntry<String, Integer>(hostName, port), connectionStatus);
    }

    public Map<Map.Entry<String, String>, ConnectionStatus> getDiscoveredDataSources() {
        return discoveredDataSources;
    }

    public void setDataSourceStatus(String url, String userName, ConnectionStatus status) {
        discoveredDataSources.put(new AbstractMap.SimpleEntry<String, String>(url, userName), status);
    }

    public boolean isPersistRegistry() {
        return persistRegistry;
    }

    public void setPersistRegistry(boolean persistRegistry) {
        this.persistRegistry = persistRegistry;
    }

    public void clear() {
        discoveredAdresses.clear();
        discoveredDataSources.clear();
    }

    public void writeTo(Writer writer) throws IOException {

        writer.write("{");

        if (!discoveredAdresses.isEmpty()) {

            writer.write("\"sockets\":[");

            Iterator<Map.Entry<Map.Entry<String, Integer>, ConnectionStatus>> iterator =
                    discoveredAdresses.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Map.Entry<String,Integer>, ConnectionsRegistry.ConnectionStatus> entry = iterator.next();

                String hostName = entry.getKey().getKey();
                Integer port = entry.getKey().getValue();

                writer.write('{');
                if (null != hostName) {
                    writer.write("\"host\":\"");
                    writer.write(hostName);
                    writer.write("\"");
                }
                if (null != port) {
                    if (null != hostName) writer.write(',');
                    writer.write("\"port\":\"");
                    writer.write(port.toString());
                    writer.write("\"");
                }
                writer.write(',');
                writer.write("\"status\":\"");
                writer.write(entry.getValue().name());
                writer.write("\"");
                writer.write('}');
                if (iterator.hasNext()) writer.write(',');

            }

            writer.write(']');

            writer.flush();

        }

        if (!discoveredDataSources.isEmpty()) {

            if (!discoveredAdresses.isEmpty()) {
                writer.write(',');
            }

            writer.write("\"dataSources\":[");

            Iterator<Map.Entry<Map.Entry<String, String>, ConnectionsRegistry.ConnectionStatus>> iterator =
                    discoveredDataSources.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Map.Entry<String,String>, ConnectionsRegistry.ConnectionStatus> entry = iterator.next();

                String url = entry.getKey().getKey();
                String userName = entry.getKey().getValue();

                writer.write('{');
                if (null != url) {
                    writer.write("\"url\":\"");
                    writer.write(url);
                    writer.write("\"");
                }
                if (null != userName) {
                    if (null != url) writer.write(',');
                    writer.write("\"userName\":\"");
                    writer.write(userName);
                    writer.write("\"");
                }
                writer.write(',');
                writer.write("\"status\":\"");
                writer.write(entry.getValue().name());
                writer.write("\"");
                writer.write('}');
                if (iterator.hasNext()) writer.write(',');

            }

            writer.write(']');

            writer.flush();

        }

        writer.write("}");

    }

}
