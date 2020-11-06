package io.sniffy.registry;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.util.StringUtil;

import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 3.1
 */
public enum ConnectionsRegistry implements Runnable {
    INSTANCE;

    private final Map<Map.Entry<String, Integer>, Integer> discoveredAddresses = new ConcurrentHashMap<Map.Entry<String, Integer>, Integer>();
    private final Map<Map.Entry<String, String>, Integer> discoveredDataSources = new ConcurrentHashMap<Map.Entry<String, String>, Integer>();

    // visible for testing
    protected final Map<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> sniffySocketImpls =
            new ConcurrentHashMap<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>>();

    private volatile boolean persistRegistry = false;

    private final ReferenceQueue<SniffyNetworkConnection> sniffySocketReferenceQueue = new ReferenceQueue<SniffyNetworkConnection>();

    private final Thread housekeepingThread = new Thread(this, "SniffyConnectionRegistryHouseKeeper");

    private final ThreadLocal<Map<Map.Entry<String, Integer>, Integer>> threadLocalDiscoveredAddresses =
            new ThreadLocal<Map<Map.Entry<String, Integer>, Integer>>() {

                @Override
                protected Map<Map.Entry<String, Integer>, Integer> initialValue() {
                    return new ConcurrentHashMap<Map.Entry<String, Integer>, Integer>();
                }

            };

    private final ThreadLocal<Map<Map.Entry<String, String>, Integer>> threadLocalDiscoveredDataSources =
            new ThreadLocal<Map<Map.Entry<String, String>, Integer>>() {

                @Override
                protected Map<Map.Entry<String, String>, Integer> initialValue() {
                    return new ConcurrentHashMap<Map.Entry<String, String>, Integer>();
                }
            };

    private volatile boolean threadLocal = false;

    public void setThreadLocalDiscoveredAddresses(Map<Map.Entry<String, Integer>, Integer> discoveredAddresses) {
        threadLocalDiscoveredAddresses.set(discoveredAddresses);
    }

    public void setThreadLocalDiscoveredDataSources(Map<Map.Entry<String, String>, Integer> discoveredDataSources) {
        threadLocalDiscoveredDataSources.set(discoveredDataSources);
    }

    ConnectionsRegistry() {
        try {
            ConnectionsRegistryStorage.INSTANCE.loadConnectionsRegistry(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        housekeepingThread.setDaemon(true);
        housekeepingThread.start();
    }

    public Integer resolveDataSourceStatus(String url, String userName) {

        Map<Map.Entry<String, String>, Integer> discoveredDataSources = getDiscoveredDataSources();

        for (Map.Entry<Map.Entry<String, String>, Integer> entry : discoveredDataSources.entrySet()) {

            if ((null == url || url.equals(entry.getKey().getKey())) &&
                    (null == userName || userName.equals(entry.getKey().getValue())) &&
                    0 != entry.getValue()) {
                return entry.getValue();
            }

        }

        setDataSourceStatus(url, userName, 0);

        return 0;

    }

    public int resolveSocketAddressStatus(InetSocketAddress inetSocketAddress, SniffyNetworkConnection sniffyNetworkConnection) {

        if (null == inetSocketAddress || null == inetSocketAddress.getAddress()) {
            return 0;
        }

        Map<Map.Entry<String, Integer>, Integer> discoveredAddresses = getDiscoveredAddresses();

        InetAddress inetAddress = inetSocketAddress.getAddress();

        if (null != sniffyNetworkConnection && !threadLocal) {
            {
                AbstractMap.SimpleEntry<String, Integer> hostNamePortPair = new AbstractMap.SimpleEntry<String, Integer>(inetAddress.getHostName(), inetSocketAddress.getPort());
                Collection<Reference<SniffyNetworkConnection>> sniffySockets = sniffySocketImpls.get(hostNamePortPair);
                if (null == sniffySockets) {
                    synchronized (sniffySocketImpls) {
                        sniffySockets = sniffySocketImpls.get(hostNamePortPair);
                        if (null == sniffySockets) {
                            sniffySockets = Collections.newSetFromMap(new ConcurrentHashMap<Reference<SniffyNetworkConnection>, Boolean>());
                            sniffySocketImpls.put(hostNamePortPair, sniffySockets);
                        }
                    }
                }
                sniffySockets.add(new WeakReference<SniffyNetworkConnection>(sniffyNetworkConnection, sniffySocketReferenceQueue));
            }
            {
                AbstractMap.SimpleEntry<String, Integer> hostAddressPortPair = new AbstractMap.SimpleEntry<String, Integer>(inetAddress.getHostAddress(), inetSocketAddress.getPort());
                Collection<Reference<SniffyNetworkConnection>> sniffySockets = sniffySocketImpls.get(hostAddressPortPair);
                if (null == sniffySockets) {
                    synchronized (sniffySocketImpls) {
                        sniffySockets = sniffySocketImpls.get(hostAddressPortPair);
                        if (null == sniffySockets) {
                            sniffySockets = Collections.newSetFromMap(new ConcurrentHashMap<Reference<SniffyNetworkConnection>, Boolean>());
                            sniffySocketImpls.put(hostAddressPortPair, sniffySockets);
                        }
                    }
                }
                sniffySockets.add(new WeakReference<SniffyNetworkConnection>(sniffyNetworkConnection, sniffySocketReferenceQueue));
            }
        }

        for (Map.Entry<Map.Entry<String, Integer>, Integer> entry : discoveredAddresses.entrySet()) {

            String hostName = entry.getKey().getKey();
            Integer port = entry.getKey().getValue();

            if ((null == hostName || hostName.equals(inetAddress.getHostName()) || hostName.equals(inetAddress.getHostAddress())) &&
                    (null == port || port == inetSocketAddress.getPort()) &&
                    0 != entry.getValue()) {
                return entry.getValue();
            }

        }

        setSocketAddressStatus(inetSocketAddress.getHostName(), inetSocketAddress.getPort(), 0);

        return 0;

    }

    public Map<Map.Entry<String, Integer>, Integer> getDiscoveredAddresses() {
        return Collections.unmodifiableMap(getDiscoveredAddressesImpl());
    }

    private Map<Map.Entry<String, Integer>, Integer> getDiscoveredAddressesImpl() {
        return threadLocal ? threadLocalDiscoveredAddresses.get() : this.discoveredAddresses;
    }

    public void setSocketAddressStatus(String hostName, Integer port, Integer connectionStatus) {

        Map<Map.Entry<String, Integer>, Integer> discoveredAddresses = getDiscoveredAddressesImpl();

        discoveredAddresses.put(new AbstractMap.SimpleEntry<String, Integer>(hostName, port), connectionStatus);

        if (persistRegistry) {
            try {
                ConnectionsRegistryStorage.INSTANCE.storeConnectionsRegistry(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Collection<Reference<SniffyNetworkConnection>> sniffySockets = sniffySocketImpls.get(new AbstractMap.SimpleEntry<String, Integer>(hostName, port));
        if (null != sniffySockets) {
            for (Reference<SniffyNetworkConnection> sniffySocketWeakReference : sniffySockets) {
                SniffyNetworkConnection sniffyNetworkConnection = sniffySocketWeakReference.get();
                if (null != sniffyNetworkConnection) {
                    sniffyNetworkConnection.setConnectionStatus(connectionStatus);
                }
            }
        }

    }

    public Map<Map.Entry<String, String>, Integer> getDiscoveredDataSources() {
        return threadLocal ? threadLocalDiscoveredDataSources.get() : this.discoveredDataSources;
    }

    public void setDataSourceStatus(String url, String userName, Integer status) {

        Map<Map.Entry<String, String>, Integer> discoveredDataSources = getDiscoveredDataSources();

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
        sniffySocketImpls.clear();
    }

    public void readFrom(Reader reader) throws IOException {

        JsonObject json = Json.parse(reader).asObject();

        if (null != json.get("sockets")) {
            JsonArray sockets = json.get("sockets").asArray();
            for (int i = 0; i < sockets.size(); i++) {
                JsonObject socket = sockets.get(i).asObject();
                String hostName = socket.get("host").asString();
                int port = Integer.parseInt(socket.get("port").asString());
                Integer connectionStatus = socket.get("status").asInt();
                discoveredAddresses.put(new AbstractMap.SimpleEntry<String, Integer>(hostName, port), connectionStatus);
            }
        }

        if (null != json.get("dataSources")) {
            JsonArray dataSources = json.get("dataSources").asArray();
            for (int i = 0; i < dataSources.size(); i++) {
                JsonObject dataSource = dataSources.get(i).asObject();
                String url = dataSource.get("url").asString();
                String userName = dataSource.get("userName").asString();
                Integer connectionStatus = dataSource.get("status").asInt();
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

        Map<Map.Entry<String, Integer>, Integer> discoveredAddresses = getDiscoveredAddresses();
        Map<Map.Entry<String, String>, Integer> discoveredDataSources = getDiscoveredDataSources();

        writer.write("{");

        writer.write("\"persistent\":");
        writer.write(Boolean.toString(persistRegistry));

        if (!discoveredAddresses.isEmpty()) {

            writer.write(",\"sockets\":[");

            Iterator<Map.Entry<Map.Entry<String, Integer>, Integer>> iterator =
                    discoveredAddresses.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Map.Entry<String, Integer>, Integer> entry = iterator.next();

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
                writer.write(entry.getValue().toString());
                writer.write('}');
                if (iterator.hasNext()) writer.write(',');

            }

            writer.write(']');

            writer.flush();

        }

        if (!discoveredDataSources.isEmpty()) {

            writer.write(",\"dataSources\":[");

            Iterator<Map.Entry<Map.Entry<String, String>, Integer>> iterator =
                    discoveredDataSources.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Map.Entry<String, String>, Integer> entry = iterator.next();

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
                writer.write(entry.getValue().toString());
                writer.write('}');
                if (iterator.hasNext()) writer.write(',');

            }

            writer.write(']');

            writer.flush();

        }

        writer.write("}");

    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Reference<? extends SniffyNetworkConnection> reference = sniffySocketReferenceQueue.remove();
                SniffyNetworkConnection sniffyNetworkConnection = reference.get();
                if (null != sniffyNetworkConnection) {
                    InetSocketAddress inetSocketAddress = sniffyNetworkConnection.getInetSocketAddress();

                    {
                        Collection<Reference<SniffyNetworkConnection>> sniffySockets = sniffySocketImpls.get(
                                new AbstractMap.SimpleEntry<String, Integer>(
                                        inetSocketAddress.getAddress().getHostName(), inetSocketAddress.getPort()
                                )
                        );
                        if (null != sniffySockets) {
                            sniffySockets.remove(reference);
                        }
                    }

                    {
                        Collection<Reference<SniffyNetworkConnection>> sniffySockets = sniffySocketImpls.get(
                                new AbstractMap.SimpleEntry<String, Integer>(
                                        inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort()
                                )
                        );
                        if (null != sniffySockets) {
                            sniffySockets.remove(reference);
                        }
                    }

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

}
