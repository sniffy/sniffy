package io.sniffy.registry;

import io.sniffy.socket.SniffyNetworkConnection;

import java.lang.ref.Reference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Test-only snapshot of the registry's weak live-connection registrations. */
public final class ConnectionsRegistryTestState {

    private final ConnectionsRegistry registry;
    private final Map<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> registrations;

    private ConnectionsRegistryTestState(
            ConnectionsRegistry registry,
            Map<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> registrations) {
        this.registry = registry;
        this.registrations = registrations;
    }

    public static ConnectionsRegistryTestState capture(ConnectionsRegistry registry) {
        Map<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> snapshot =
                new LinkedHashMap<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>>();
        synchronized (registry.sniffySocketImpls) {
            for (Map.Entry<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> entry
                    : registry.sniffySocketImpls.entrySet()) {
                snapshot.put(copyEndpoint(entry.getKey()),
                        new ArrayList<Reference<SniffyNetworkConnection>>(entry.getValue()));
            }
        }
        return new ConnectionsRegistryTestState(registry, snapshot);
    }

    public void restore() {
        synchronized (registry.sniffySocketImpls) {
            registry.sniffySocketImpls.clear();
            for (Map.Entry<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> entry
                    : registrations.entrySet()) {
                Collection<Reference<SniffyNetworkConnection>> restored =
                        java.util.Collections.newSetFromMap(
                                new ConcurrentHashMap<Reference<SniffyNetworkConnection>, Boolean>());
                for (Reference<SniffyNetworkConnection> reference : entry.getValue()) {
                    // Do not resurrect a registration whose weak referent disappeared during the scope.
                    if (reference.get() != null) restored.add(reference);
                }
                if (!restored.isEmpty()) registry.sniffySocketImpls.put(copyEndpoint(entry.getKey()), restored);
            }
        }
    }

    public boolean matchesCurrentState() {
        synchronized (registry.sniffySocketImpls) {
            Map<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> live =
                    liveRegistrations();
            if (registry.sniffySocketImpls.size() != live.size()) return false;
            for (Map.Entry<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> entry
                    : live.entrySet()) {
                Collection<Reference<SniffyNetworkConnection>> current = registry.sniffySocketImpls.get(entry.getKey());
                if (current == null || !current.containsAll(entry.getValue()) || current.size() != entry.getValue().size()) {
                    return false;
                }
            }
            return true;
        }
    }

    public static int registrationCount(ConnectionsRegistry registry, SniffyNetworkConnection connection) {
        int count = 0;
        synchronized (registry.sniffySocketImpls) {
            for (Collection<Reference<SniffyNetworkConnection>> references : registry.sniffySocketImpls.values()) {
                for (Reference<SniffyNetworkConnection> reference : references) {
                    if (reference.get() == connection) count++;
                }
            }
        }
        return count;
    }

    private Map<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> liveRegistrations() {
        Map<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> live =
                new LinkedHashMap<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>>();
        for (Map.Entry<Map.Entry<String, Integer>, Collection<Reference<SniffyNetworkConnection>>> entry
                : registrations.entrySet()) {
            Collection<Reference<SniffyNetworkConnection>> references =
                    new ArrayList<Reference<SniffyNetworkConnection>>();
            for (Reference<SniffyNetworkConnection> reference : entry.getValue()) {
                if (reference.get() != null) references.add(reference);
            }
            if (!references.isEmpty()) live.put(entry.getKey(), references);
        }
        return live;
    }

    private static Map.Entry<String, Integer> copyEndpoint(Map.Entry<String, Integer> endpoint) {
        return new AbstractMap.SimpleImmutableEntry<String, Integer>(endpoint.getKey(), endpoint.getValue());
    }
}
