package io.sniffy.registry;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.sniffy.util.StringUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.sniffy.registry.ConnectionsRegistry.ConnectionStatus.OPEN;

/**
 * @since 3.1
 */
public enum ConnectionsRegistry {
    INSTANCE;

    public enum ConnectionStatus {
        OPEN,
        CLOSED
    }

    private final Map<Map.Entry<String,Integer>, ConnectionStatus> discoveredAddresses = new ConcurrentHashMap<Map.Entry<String,Integer>, ConnectionStatus>();
    private final Map<Map.Entry<String,String>, ConnectionStatus> discoveredDataSources = new ConcurrentHashMap<Map.Entry<String,String>, ConnectionStatus>();

    private volatile boolean persistRegistry = false;

    private final ThreadLocal<Map<Map.Entry<String,Integer>, ConnectionStatus>> threadLocalDiscoveredAddresses =
            new ThreadLocal<Map<Map.Entry<String,Integer>, ConnectionStatus>>() {

                @Override
                protected Map<Map.Entry<String, Integer>, ConnectionStatus> initialValue() {
                    return new ConcurrentHashMap<Map.Entry<String,Integer>, ConnectionStatus>();
                }

            };

    private final ThreadLocal<Map<Map.Entry<String,String>, ConnectionStatus>> threadLocalDiscoveredDataSources =
            new ThreadLocal<Map<Map.Entry<String,String>, ConnectionStatus>>() {

                @Override
                protected Map<Map.Entry<String, String>, ConnectionStatus> initialValue() {
                    return new ConcurrentHashMap<Map.Entry<String,String>, ConnectionStatus>();
                }
            };

    private volatile boolean threadLocal = false;

    public void setThreadLocalDiscoveredAddresses(Map<Map.Entry<String, Integer>, ConnectionStatus> discoveredAddresses) {
        threadLocalDiscoveredAddresses.set(discoveredAddresses);
    }

    public void setThreadLocalDiscoveredDataSources(Map<Map.Entry<String,String>, ConnectionStatus> discoveredDataSources) {
        threadLocalDiscoveredDataSources.set(discoveredDataSources);
    }

    ConnectionsRegistry() {
        try {
            ConnectionsRegistryStorage.INSTANCE.loadConnectionsRegistry(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ConnectionStatus resolveDataSourceStatus(String url, String userName) {

        Map<Map.Entry<String, String>, ConnectionStatus> discoveredDataSources = getDiscoveredDataSources();

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

        if (null == inetSocketAddress || null == inetSocketAddress.getAddress()) {
            return OPEN;
        }

        Map<Map.Entry<String, Integer>, ConnectionStatus> discoveredAddresses = getDiscoveredAddresses();

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
        return threadLocal ? threadLocalDiscoveredAddresses.get() : this.discoveredAddresses;
    }

    public void setSocketAddressStatus(String hostName, Integer port, ConnectionStatus connectionStatus) {

        Map<Map.Entry<String, Integer>, ConnectionStatus> discoveredAddresses = getDiscoveredAddresses();

        discoveredAddresses.put(new AbstractMap.SimpleEntry<String, Integer>(hostName, port), connectionStatus);
        if (persistRegistry) {
            try {
                ConnectionsRegistryStorage.INSTANCE.storeConnectionsRegistry(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<Map.Entry<String, String>, ConnectionStatus> getDiscoveredDataSources() {
        return threadLocal ? threadLocalDiscoveredDataSources.get() : this.discoveredDataSources;
    }

    public void setDataSourceStatus(String url, String userName, ConnectionStatus status) {

        Map<Map.Entry<String, String>, ConnectionStatus> discoveredDataSources = getDiscoveredDataSources();

        discoveredDataSources.put(new AbstractMap.SimpleEntry<String, String>(url, userName), status);
        if (persistRegistry) {
            try {
                ConnectionsRegistryStorage.INSTANCE.storeConnectionsRegistry(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isPersistRegistry() {
        return persistRegistry;
    }

    public void setPersistRegistry(boolean persistRegistry) {
        this.persistRegistry = persistRegistry;
    }

    public boolean isThreadLocal() {
        return threadLocal;
    }

    public void setThreadLocal(boolean threadLocal) {
        this.threadLocal = threadLocal;
    }

    public void clear() {
        discoveredAddresses.clear();
        discoveredDataSources.clear();
        persistRegistry = false;
    }

    public void readFrom(Reader reader) throws IOException {

        JsonObject json = Json.parse(reader).asObject();

        if (null != json.get("sockets")) {
            JsonArray sockets = json.get("sockets").asArray();
            for (int i = 0; i < sockets.size(); i++) {
                JsonObject socket = sockets.get(i).asObject();
                String hostName = socket.get("host").asString();
                int port = Integer.parseInt(socket.get("port").asString());
                ConnectionStatus connectionStatus = ConnectionStatus.valueOf(socket.get("status").asString());
                discoveredAddresses.put(new AbstractMap.SimpleEntry<String, Integer>(hostName, port), connectionStatus);
            }
        }

        if (null != json.get("dataSources")) {
            JsonArray dataSources = json.get("dataSources").asArray();
            for (int i = 0; i < dataSources.size(); i++) {
                JsonObject dataSource = dataSources.get(i).asObject();
                String url = dataSource.get("url").asString();
                String userName = dataSource.get("userName").asString();
                ConnectionStatus connectionStatus = ConnectionStatus.valueOf(dataSource.get("status").asString());
                discoveredDataSources.put(new AbstractMap.SimpleEntry<String, String>(url, userName), connectionStatus);
            }
        }

    }

    public void writeTo(OutputStream outputStream, String charset) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, charset);
        writeTo(writer);
        writer.flush();
    }

    public void writeTo(Writer writer) throws IOException {

        Map<Map.Entry<String, Integer>, ConnectionStatus> discoveredAddresses = getDiscoveredAddresses();
        Map<Map.Entry<String, String>, ConnectionStatus> discoveredDataSources = getDiscoveredDataSources();

        writer.write("{");

        writer.write("\"persistent\":");
        writer.write(Boolean.toString(persistRegistry));

        if (!discoveredAddresses.isEmpty()) {

            writer.write(",\"sockets\":[");

            Iterator<Map.Entry<Map.Entry<String, Integer>, ConnectionStatus>> iterator =
                    discoveredAddresses.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Map.Entry<String,Integer>, ConnectionsRegistry.ConnectionStatus> entry = iterator.next();

                String hostName = entry.getKey().getKey();
                Integer port = entry.getKey().getValue();

                writer.write('{');
                if (null != hostName) {
                    writer.write("\"host\":");
                    writer.write(StringUtil.escapeJsonString(hostName));
                }
                if (null != port) {
                    if (null != hostName) writer.write(',');
                    writer.write("\"port\":\"");
                    writer.write(port.toString());
                    writer.write("\"");
                }
                writer.write(',');
                writer.write("\"status\":");
                writer.write(StringUtil.escapeJsonString(entry.getValue().name()));
                writer.write('}');
                if (iterator.hasNext()) writer.write(',');

            }

            writer.write(']');

            writer.flush();

        }

        if (!discoveredDataSources.isEmpty()) {

            writer.write(",\"dataSources\":[");

            Iterator<Map.Entry<Map.Entry<String, String>, ConnectionsRegistry.ConnectionStatus>> iterator =
                    discoveredDataSources.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Map.Entry<String,String>, ConnectionsRegistry.ConnectionStatus> entry = iterator.next();

                String url = entry.getKey().getKey();
                String userName = entry.getKey().getValue();

                writer.write('{');
                if (null != url) {
                    writer.write("\"url\":");
                    writer.write(StringUtil.escapeJsonString(url));
                }
                if (null != userName) {
                    if (null != url) writer.write(',');
                    writer.write("\"userName\":");
                    writer.write(StringUtil.escapeJsonString(userName));
                }
                writer.write(',');
                writer.write("\"status\":");
                writer.write(StringUtil.escapeJsonString(entry.getValue().name()));
                writer.write('}');
                if (iterator.hasNext()) writer.write(',');

            }

            writer.write(']');

            writer.flush();

        }

        writer.write("}");

    }

}
