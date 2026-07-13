package io.sniffy.nio;

import java.nio.channels.spi.SelectorProvider;

/** Minimal provider-slot capability used to keep installation atomic and testable. */
interface NioProviderAccess {
    SelectorProvider getSelectorProvider();
    void setSelectorProvider(SelectorProvider provider);
    String describeProviderSlot();
}

interface NioProviderAccessResolver {
    NioProviderAccess resolve() throws JdkNioAccess.JdkNioAccessException;
}
