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

        if (null == inetSocketAddress || null == inetSocketAddress.getAddress()) { // TODO: can be null for unresolved addresses
            return 0;
        }

        Map<Map.Entry<String, Integer>, Integer> discoveredAddresses = getDiscoveredAddresses();

        InetAddress inetAddress = inetSocketAddress.getAddress();

        if (null != sniffyNetworkConnection && !threadLocal) {
            registerNetworkConnection(new AbstractMap.SimpleEntry<String, Integer>(
                    inetAddress.getHostName(), inetSocketAddress.getPort()), sniffyNetworkConnection);
            registerNetworkConnection(new AbstractMap.SimpleEntry<String, Integer>(
                    inetAddress.getHostAddress(), inetSocketAddress.getPort()), sniffyNetworkConnection);
        }

        // search for given address in discoveredAddresses map (global or thread local)
        for (Map.Entry<Map.Entry<String, Integer>, Integer> entry : discoveredAddresses.entrySet()) {

            String hostName = entry.getKey().getKey();
            Integer port = entry.getKey().getValue();

            if ((null == hostName || hostName.equals(inetAddress.getHostName()) || hostName.equals(inetAddress.getHostAddress())) &&
                    (null == port || port == inetSocketAddress.getPort()) &&
                    0 != entry.getValue()) {
                return entry.getValue();
            }

        }

        // store given address with 0 connection status (allowed without delay) to discoveredAddresses map (global)
        setSocketAddressStatus(inetSocketAddress.getHostName(), inetSocketAddress.getPort(), 0);

        // return 0 - connection allowed without delay
        return 0;

    }

    private void registerNetworkConnection(Map.Entry<String, Integer> endpoint,
                                           SniffyNetworkConnection connection) {
        synchronized (sniffySocketImpls) {
            Collection<Reference<SniffyNetworkConnection>> references = sniffySocketImpls.get(endpoint);
            if (references == null) {
                references = Collections.newSetFromMap(
                        new ConcurrentHashMap<Reference<SniffyNetworkConnection>, Boolean>());
                sniffySocketImpls.put(endpoint, references);
            }
            boolean alreadyRegistered = false;
            for (Iterator<Reference<SniffyNetworkConnection>> iterator = references.iterator(); iterator.hasNext();) {
                SniffyNetworkConnection registered = iterator.next().get();
                if (registered == connection) alreadyRegistered = true;
                if (registered == null) iterator.remove();
            }
            if (!alreadyRegistered) {
                references.add(new WeakReference<SniffyNetworkConnection>(connection, sniffySocketReferenceQueue));
            }
        }
    }

    /**
     * Removes every hostname/IP alias registered for this connection. Identity comparison is intentional:
     * connection implementations are not required to define value equality.
     *
     * @since 3.2
     */
    public void unregisterNetworkConnection(SniffyNetworkConnection connection) {
        if (connection == null) return;
        synchronized (sniffySocketImpls) {
            for (Map.Entry<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> entry
                    : sniffySocketImpls.entrySet()) {
                Collection<Reference<SniffyNetworkConnection>> references = entry.getValue();
                for (Iterator<Reference<SniffyNetworkConnection>> iterator = references.iterator(); iterator.hasNext();) {
                    SniffyNetworkConnection registered = iterator.next().get();
                    if (registered == null || registered == connection) iterator.remove();
                }
                if (references.isEmpty() && sniffySocketImpls.get(entry.getKey()) == references) {
                    sniffySocketImpls.remove(entry.getKey());
                }
            }
        }
    }

    public Map<Map.Entry<String, Integer>, Integer> getDiscoveredAddresses() {
        return Collections.unmodifiableMap(getDiscoveredAddressesImpl());
    }

    private Map<Map.Entry<String, Integer>, Integer> getDiscoveredAddressesImpl() {
        return threadLocal ? threadLocalDiscoveredAddresses.get() : this.discoveredAddresses;
    }

    public void setSocketAddressStatus(String hostName, Integer port, Integer connectionStatus) {
        setSocketAddressStatus(hostName, port, connectionStatus, true);
    }

    void setSocketAddressStatus(String hostName, Integer port, Integer connectionStatus, boolean persistChange) {

        Map<Map.Entry<String, Integer>, Integer> discoveredAddresses = getDiscoveredAddressesImpl();

        discoveredAddresses.put(new AbstractMap.SimpleEntry<String, Integer>(hostName, port), connectionStatus);

        if (persistChange && persistRegistry) {
            try {
                ConnectionsRegistryStorage.INSTANCE.storeConnectionsRegistry(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Set<SniffyNetworkConnection> matchingConnections =
                Collections.newSetFromMap(new IdentityHashMap<SniffyNetworkConnection, Boolean>());
        synchronized (sniffySocketImpls) {
            if (hostName != null && port != null) {
                collectConnections(sniffySocketImpls.get(
                        new AbstractMap.SimpleEntry<String, Integer>(hostName, port)), matchingConnections);
            } else {
                for (Map.Entry<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> entry
                        : sniffySocketImpls.entrySet()) {
                    String registeredHost = entry.getKey().getKey();
                    Integer registeredPort = entry.getKey().getValue();
                    if ((hostName == null || hostName.equals(registeredHost))
                            && (port == null || port.equals(registeredPort))) {
                        collectConnections(entry.getValue(), matchingConnections);
                    }
                }
            }
        }
        for (SniffyNetworkConnection connection : matchingConnections) {
            if (hostName != null && port != null) {
                connection.setConnectionStatus(new InetSocketAddress(hostName, port), connectionStatus);
            } else {
                // Endpoint-less registry rules apply to whichever endpoint is currently effective.
                connection.setConnectionStatus(connectionStatus);
            }
        }

    }

    private static void collectConnections(Collection<Reference<SniffyNetworkConnection>> references,
                                           Set<SniffyNetworkConnection> connections) {
        if (references == null) return;
        for (Reference<SniffyNetworkConnection> reference : references) {
            SniffyNetworkConnection connection = reference.get();
            if (connection != null) connections.add(connection);
        }
    }

    public Map<Map.Entry<String, String>, Integer> getDiscoveredDataSources() {
        return getDiscoveredDataSourcesImpl();
    }

    private Map<Map.Entry<String, String>, Integer> getDiscoveredDataSourcesImpl() {
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


    /**
     * Captures the registry state affected by scoped connectivity overrides.
     *
     * @since 4.0
     */
    public ConnectionsRegistrySnapshot takeSnapshot() {
        return new ConnectionsRegistrySnapshot(this);
    }

    /**
     * Restores a snapshot captured with {@link #takeSnapshot()} without clearing unrelated global state first.
     *
     * @since 4.0
     */
    public void restoreSnapshot(ConnectionsRegistrySnapshot snapshot) {
        if (snapshot != null) {
            snapshot.restore(this);
        }
    }

    public static final class ConnectionsRegistrySnapshot {
        private final boolean persistRegistry;
        private final boolean threadLocal;
        private final Map<Map.Entry<String, Integer>, Integer> addresses;
        private final Map<Map.Entry<String, String>, Integer> dataSources;
        private final Map<Map.Entry<String, Integer>, Collection<SniffyNetworkConnection>> connections;
        private final ConnectionsRegistryStorage.StorageSnapshot storageSnapshot;

        private ConnectionsRegistrySnapshot(ConnectionsRegistry registry) {
            this.persistRegistry = registry.persistRegistry;
            this.threadLocal = registry.threadLocal;
            this.addresses = new LinkedHashMap<Map.Entry<String, Integer>, Integer>(registry.getDiscoveredAddressesImpl());
            this.dataSources = new LinkedHashMap<Map.Entry<String, String>, Integer>(registry.getDiscoveredDataSourcesImpl());
            this.connections = new LinkedHashMap<Map.Entry<String, Integer>, Collection<SniffyNetworkConnection>>();
            this.storageSnapshot = ConnectionsRegistryStorage.INSTANCE.takeStorageSnapshot();
            synchronized (registry.sniffySocketImpls) {
                for (Map.Entry<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> entry : registry.sniffySocketImpls.entrySet()) {
                    Collection<SniffyNetworkConnection> liveConnections = new ArrayList<SniffyNetworkConnection>();
                    for (Reference<SniffyNetworkConnection> reference : entry.getValue()) {
                        SniffyNetworkConnection connection = reference.get();
                        if (connection != null) {
                            liveConnections.add(connection);
                        }
                    }
                    if (!liveConnections.isEmpty()) {
                        this.connections.put(entry.getKey(), liveConnections);
                    }
                }
            }
        }

        private void restore(ConnectionsRegistry registry) {
            Throwable failure = null;
            List<Throwable> failures = new ArrayList<Throwable>();
            try {
                registry.threadLocal = this.threadLocal;
                restoreMap(registry.getDiscoveredAddressesImpl(), this.addresses);
                restoreMap(registry.getDiscoveredDataSourcesImpl(), this.dataSources);
                synchronized (registry.sniffySocketImpls) {
                    registry.sniffySocketImpls.clear();
                    for (Map.Entry<Map.Entry<String, Integer>, Collection<SniffyNetworkConnection>> entry : this.connections.entrySet()) {
                        Collection<Reference<SniffyNetworkConnection>> references = Collections.newSetFromMap(
                                new ConcurrentHashMap<Reference<SniffyNetworkConnection>, Boolean>());
                        for (SniffyNetworkConnection connection : entry.getValue()) {
                            references.add(new WeakReference<SniffyNetworkConnection>(connection, registry.sniffySocketReferenceQueue));
                        }
                        registry.sniffySocketImpls.put(entry.getKey(), references);
                    }
                }
                registry.persistRegistry = this.persistRegistry;
            } catch (Throwable t) {
                failure = suppress(failure, t);
                failures.add(t);
            }
            for (Map.Entry<Map.Entry<String, Integer>, Collection<SniffyNetworkConnection>> entry : this.connections.entrySet()) {
                Integer status = this.addresses.get(entry.getKey());
                if (status == null) {
                    status = 0;
                }
                for (SniffyNetworkConnection connection : entry.getValue()) {
                    try {
                        connection.setConnectionStatus(new InetSocketAddress(entry.getKey().getKey(), entry.getKey().getValue()), status);
                    } catch (Throwable t) {
                        failure = suppress(failure, t);
                        failures.add(t);
                    }
                }
            }
            try {
                ConnectionsRegistryStorage.INSTANCE.restoreStorageSnapshot(this.storageSnapshot);
            } catch (Throwable t) {
                failure = suppress(failure, t);
                failures.add(t);
            }
            if (failure != null) {
                throw new ConnectionsRegistryRestoreException(failures);
            }
        }

        private static Throwable suppress(Throwable failure, Throwable t) {
            return failure == null ? t : failure;
        }

        private static <K, V> void restoreMap(Map<K, V> target, Map<K, V> snapshot) {
            for (Iterator<K> iterator = target.keySet().iterator(); iterator.hasNext();) {
                K key = iterator.next();
                if (!snapshot.containsKey(key)) {
                    iterator.remove();
                }
            }
            target.putAll(snapshot);
        }
    }

    public static final class ConnectionsRegistryRestoreException extends RuntimeException {
        private final List<Throwable> failures;

        private ConnectionsRegistryRestoreException(List<Throwable> failures) {
            super(failures.get(0).getMessage(), failures.get(0));
            this.failures = new ArrayList<Throwable>(failures);
        }

        public List<Throwable> getFailures() {
            return Collections.unmodifiableList(failures);
        }
    }

    /**
     * Applies a temporary socket status without persisting the scoped override.
     *
     * @since 4.0
     */
    public void setSocketAddressStatusVolatile(String hostName, Integer port, Integer connectionStatus) {
        setSocketAddressStatus(hostName, port, connectionStatus, false);
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
        synchronized (sniffySocketImpls) {
            sniffySocketImpls.clear();
        }
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
                // An enqueued WeakReference has already lost its referent, so remove the reference
                // object itself from every alias bucket instead of trying to recover its endpoint.
                synchronized (sniffySocketImpls) {
                    for (Map.Entry<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> entry
                            : sniffySocketImpls.entrySet()) {
                        Collection<Reference<SniffyNetworkConnection>> references = entry.getValue();
                        references.remove(reference);
                        if (references.isEmpty() && sniffySocketImpls.get(entry.getKey()) == references) {
                            sniffySocketImpls.remove(entry.getKey());
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

}
