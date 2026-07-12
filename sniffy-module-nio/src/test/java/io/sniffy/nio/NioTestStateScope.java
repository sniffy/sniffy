package io.sniffy.nio;

import io.sniffy.Sniffy;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.SniffySSLNetworkConnection;

import java.nio.ByteBuffer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Restores locally scoped static test state in reverse order without skipping later cleanup. */
final class NioTestStateScope implements AutoCloseable {

    private final List<Cleanup> cleanups = new ArrayList<Cleanup>();

    NioTestStateScope restore(Cleanup cleanup) {
        cleanups.add(cleanup);
        return this;
    }

    NioTestStateScope preserveSocketFaultInjection() {
        final Boolean previous = SniffyConfiguration.INSTANCE.getSocketFaultInjectionEnabled();
        return restore(new Cleanup() {
            @Override public void run() {
                SniffyConfiguration.INSTANCE.setSocketFaultInjectionEnabled(previous);
            }
        });
    }

    NioTestStateScope preserveConnectionsRegistry() {
        final ConnectionsRegistry registry = ConnectionsRegistry.INSTANCE;
        final boolean previousThreadLocal = registry.isThreadLocal();
        final boolean previousPersist = registry.isPersistRegistry();
        final Map<Map.Entry<String, Integer>, Integer> globalAddresses;
        final Map<Map.Entry<String, String>, Integer> globalDataSources;
        final Map<Map.Entry<String, Integer>, Integer> threadAddresses;
        final Map<Map.Entry<String, String>, Integer> threadDataSources;
        try {
            registry.setThreadLocal(false);
            globalAddresses = copy(registry.getDiscoveredAddresses());
            globalDataSources = copy(registry.getDiscoveredDataSources());
            registry.setThreadLocal(true);
            threadAddresses = copy(registry.getDiscoveredAddresses());
            threadDataSources = copy(registry.getDiscoveredDataSources());
        } finally {
            registry.setThreadLocal(previousThreadLocal);
        }
        return restore(new Cleanup() {
            @Override public void run() {
                replaceGlobalMap(registry, "discoveredAddresses", globalAddresses);
                replaceGlobalMap(registry, "discoveredDataSources", globalDataSources);
                registry.setThreadLocalDiscoveredAddresses(copy(threadAddresses));
                registry.setThreadLocalDiscoveredDataSources(copy(threadDataSources));
                registry.setPersistRegistry(previousPersist);
                registry.setThreadLocal(previousThreadLocal);

                assertEquals("global socket rules", globalAddresses,
                        mapWithMode(registry, false, true));
                assertEquals("global data-source rules", globalDataSources,
                        mapWithMode(registry, false, false));
                assertEquals("thread-local socket rules", threadAddresses,
                        mapWithMode(registry, true, true));
                assertEquals("thread-local data-source rules", threadDataSources,
                        mapWithMode(registry, true, false));
                registry.setThreadLocal(previousThreadLocal);
            }
        });
    }

    NioTestStateScope preserveClientHello(ByteBuffer key) {
        final ByteBuffer stableKey = key.duplicate();
        final boolean present = Sniffy.CLIENT_HELLO_CACHE.containsKey(stableKey);
        final SniffySSLNetworkConnection previous = Sniffy.CLIENT_HELLO_CACHE.get(stableKey);
        return restore(new Cleanup() {
            @Override public void run() {
                if (present) Sniffy.CLIENT_HELLO_CACHE.put(stableKey, previous);
                else Sniffy.CLIENT_HELLO_CACHE.remove(stableKey);
            }
        });
    }

    @Override
    public void close() throws Exception {
        Throwable failure = null;
        for (int i = cleanups.size() - 1; i >= 0; i--) {
            try {
                cleanups.get(i).run();
            } catch (Throwable e) {
                if (failure == null) failure = e;
                else if (failure != e) failure.addSuppressed(e);
            }
        }
        if (failure instanceof Exception) throw (Exception) failure;
        if (failure instanceof Error) throw (Error) failure;
        if (failure != null) throw new AssertionError(failure);
    }

    interface Cleanup {
        void run() throws Exception;
    }

    private static <K, V> Map<K, V> copy(Map<K, V> source) {
        return new HashMap<K, V>(source);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void replaceGlobalMap(ConnectionsRegistry registry, String fieldName,
                                                 Map<K, V> snapshot) {
        try {
            Field field = ConnectionsRegistry.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Map<K, V> target = (Map<K, V>) field.get(registry);
            target.clear();
            target.putAll(snapshot);
        } catch (Exception e) {
            throw new AssertionError("Cannot restore ConnectionsRegistry." + fieldName, e);
        }
    }

    private static Map<?, ?> mapWithMode(ConnectionsRegistry registry, boolean threadLocal,
                                         boolean socketRules) {
        boolean previous = registry.isThreadLocal();
        try {
            registry.setThreadLocal(threadLocal);
            if (socketRules) return copy(registry.getDiscoveredAddresses());
            return copy(registry.getDiscoveredDataSources());
        } finally {
            registry.setThreadLocal(previous);
        }
    }

    private static void assertEquals(String description, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Failed to restore " + description
                    + "; expected=" + expected + "; actual=" + actual);
        }
    }
}
