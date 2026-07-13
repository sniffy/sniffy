package io.sniffy.nio;

import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.SniffyNetworkConnection;

import java.net.InetSocketAddress;

/**
 * Owns the immutable effective-endpoint snapshots used by one monitored channel.
 * Registry resolution happens outside {@link #endpointPolicyLock}; publication uses
 * that lock only to compare and replace a complete address/status generation.
 */
final class SocketChannelEndpointPolicy {

    private final Object endpointPolicyLock = new Object();
    private final SniffyNetworkConnection connection;

    private volatile InetSocketAddress physicalAddress;
    private volatile SniffySocketChannel.EffectiveEndpointPolicy effectiveEndpointPolicy;

    SocketChannelEndpointPolicy(SniffyNetworkConnection connection, InetSocketAddress physicalAddress) {
        this.connection = connection;
        this.physicalAddress = physicalAddress;
        this.effectiveEndpointPolicy = new SniffySocketChannel.EffectiveEndpointPolicy(
                physicalAddress, null, 0L, false);
    }

    void setConnectionStatus(Integer connectionStatus) {
        synchronized (endpointPolicyLock) {
            SniffySocketChannel.EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            effectiveEndpointPolicy = current.withStatus(connectionStatus);
        }
    }

    void setConnectionStatus(InetSocketAddress endpoint, Integer connectionStatus) {
        synchronized (endpointPolicyLock) {
            SniffySocketChannel.EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            if (sameEndpoint(current.address, endpoint)) {
                effectiveEndpointPolicy = current.withStatus(connectionStatus);
            }
        }
    }

    void setProxiedAddress(InetSocketAddress proxiedAddress) {
        synchronized (endpointPolicyLock) {
            SniffySocketChannel.EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            effectiveEndpointPolicy = new SniffySocketChannel.EffectiveEndpointPolicy(
                    proxiedAddress, null, current.generation + 1L, true);
        }
    }

    void setProxiedAddressAndStatus(InetSocketAddress proxiedAddress, Integer connectionStatus) {
        synchronized (endpointPolicyLock) {
            SniffySocketChannel.EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            effectiveEndpointPolicy = new SniffySocketChannel.EffectiveEndpointPolicy(
                    proxiedAddress, connectionStatus, current.generation + 1L, true);
        }
    }

    InetSocketAddress proxiedAddress() {
        SniffySocketChannel.EffectiveEndpointPolicy snapshot = effectiveEndpointPolicy;
        return snapshot.proxied ? snapshot.address : null;
    }

    InetSocketAddress physicalAddress() {
        return physicalAddress;
    }

    void updatePhysicalAddress(InetSocketAddress address) {
        physicalAddress = address;
    }

    SniffySocketChannel.EffectiveEndpointPolicy current() {
        return effectiveEndpointPolicy;
    }

    SniffySocketChannel.EffectiveEndpointPolicy policySnapshot(
            SniffySocketChannel.EffectiveEndpointPolicy observed,
            InetSocketAddress fallbackPhysicalAddress) {
        InetSocketAddress address = observed.address == null ? fallbackPhysicalAddress : observed.address;
        return resolvePolicy(observed, address);
    }

    SniffySocketChannel.EffectiveEndpointPolicy policySnapshotForAddress(InetSocketAddress address) {
        return resolvePolicy(effectiveEndpointPolicy, address);
    }

    private SniffySocketChannel.EffectiveEndpointPolicy resolvePolicy(
            SniffySocketChannel.EffectiveEndpointPolicy observed, InetSocketAddress address) {
        if (address == null) return observed;
        boolean sameAddress = sameEndpoint(observed.address, address);
        if (sameAddress && observed.status != null && !ConnectionsRegistry.INSTANCE.isThreadLocal()) {
            return observed;
        }

        int resolved = ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(address, connection);
        SniffySocketChannel.EffectiveEndpointPolicy resolvedPolicy = new SniffySocketChannel.EffectiveEndpointPolicy(
                address, resolved, observed.generation + 1L, sameAddress && observed.proxied);
        if (ConnectionsRegistry.INSTANCE.isThreadLocal()) {
            return resolvedPolicy;
        }
        synchronized (endpointPolicyLock) {
            SniffySocketChannel.EffectiveEndpointPolicy current = effectiveEndpointPolicy;
            if (current == observed || (sameEndpoint(current.address, address) && current.status == null)) {
                effectiveEndpointPolicy = new SniffySocketChannel.EffectiveEndpointPolicy(
                        address, resolved, current.generation + 1L, current.proxied);
                return effectiveEndpointPolicy;
            }
            return current;
        }
    }

    private static boolean sameEndpoint(InetSocketAddress left, InetSocketAddress right) {
        if (left == right) return true;
        if (left == null || right == null || left.getPort() != right.getPort()) return false;
        if (left.getAddress() != null && right.getAddress() != null
                && left.getAddress().equals(right.getAddress())) return true;
        return left.getHostString().equalsIgnoreCase(right.getHostString());
    }

}
