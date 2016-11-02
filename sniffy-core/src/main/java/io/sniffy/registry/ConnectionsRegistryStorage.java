package io.sniffy.registry;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by bedrin on 02.11.2016.
 */
public enum ConnectionsRegistryStorage {
    INSTANCE;

    public void loadConnectionsRegistry() throws IOException {
        ConnectionsRegistry.INSTANCE.readFrom(new FileReader("."));
    }

    public void storeConnectionsRegistry() throws IOException {
        ConnectionsRegistry.INSTANCE.writeTo(new FileWriter("."));
    }

}
