package io.sniffy.registry;

import jodd.json.JsonParser;

import java.io.IOException;
import java.io.Reader;
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

    private final Map<Map.Entry<String,Integer>, ConnectionStatus> discoveredAddresses = new ConcurrentHashMap<Map.Entry<String,Integer>, ConnectionStatus>();
    private final Map<Map.Entry<String,String>, ConnectionStatus> discoveredDataSources = new ConcurrentHashMap<Map.Entry<String,String>, ConnectionStatus>();
    private volatile boolean persistRegistry = false;

    public ConnectionStatus resolveDataSourceStatus(String url, String userName) {

        for (Map.Entry<Map.Entry<String, String>, ConnectionStatus> entry : discoveredDataSources.entrySet()) {

            if ((null == url || url.equals(entry.getKey().getKey())) &&
                    (null == userName || userName.equals(entry.getKey().getValue())) &&
                    OPEN != entry.getValue()) {
                return entry.getValue();
            }

        }

        setDataSourceStatus(url, userName, OPEN);

        return OPEN;

    }

    public ConnectionStatus resolveSocketAddressStatus(InetSocketAddress inetSocketAddress) {

        InetAddress inetAddress = inetSocketAddress.getAddress();

        for (Map.Entry<Map.Entry<String,Integer>, ConnectionStatus> entry : discoveredAddresses.entrySet()) {

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
        return discoveredAddresses;
    }

    public void setSocketAddressStatus(String hostName, Integer port, ConnectionStatus connectionStatus) {
        discoveredAddresses.put(new AbstractMap.SimpleEntry<String, Integer>(hostName, port), connectionStatus);
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
        discoveredAddresses.clear();
        discoveredDataSources.clear();
        persistRegistry = false;
    }

    public void readFrom(Reader reader) throws IOException {
        clear();

        StringBuilder sb = new StringBuilder();
        int i;

        while((i = reader.read()) != -1) {
            sb.append((char) i);
        }

        JsonParser jsonParser = new JsonParser();
        Map map = jsonParser.parse(sb.toString());

        Object socketNodes = map.get("sockets");
        if (socketNodes instanceof Map[]) {
            for (Map socketNode : (Map[])socketNodes) {
                String hostName = (String) socketNode.get("host");
                Integer port = Integer.parseInt((String) socketNode.get("port"));
                ConnectionStatus connectionStatus = ConnectionStatus.valueOf((String) socketNode.get("status"));
                setSocketAddressStatus(hostName, port, connectionStatus);
            }
        }

        Object dataSourceNodes = map.get("dataSources");
        if (dataSourceNodes instanceof Map[]) {
            for (Map dataSourceNode : (Map[])dataSourceNodes) {
                String url = (String) dataSourceNode.get("url");
                String userName = (String) dataSourceNode.get("userName");
                ConnectionStatus connectionStatus = ConnectionStatus.valueOf((String) dataSourceNode.get("status"));
                setDataSourceStatus(url, userName, connectionStatus);
            }
        }
    }

    public void writeTo(Writer writer) throws IOException {

        writer.write("{");

        if (!discoveredAddresses.isEmpty()) {

            writer.write("\"sockets\":[");

            Iterator<Map.Entry<Map.Entry<String, Integer>, ConnectionStatus>> iterator =
                    discoveredAddresses.entrySet().iterator();

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

            if (!discoveredAddresses.isEmpty()) {
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
