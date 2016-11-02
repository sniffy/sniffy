package io.sniffy.registry;

import io.sniffy.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by bedrin on 02.11.2016.
 */
public enum ConnectionsRegistryStorage {
    INSTANCE;

    private File file = new File(IOUtil.getApplicationSniffyFolder(), "connectionsRegistry.json");

    public void loadConnectionsRegistry() throws IOException {
        FileReader reader = null;
        try {
            ConnectionsRegistry.INSTANCE.readFrom(reader = new FileReader(file));
        } finally {
            IOUtil.closeSilently(reader);
            file.delete();
        }
    }

    public void storeConnectionsRegistry() throws IOException {
        FileWriter writer = null;
        try {
            ConnectionsRegistry.INSTANCE.writeTo(writer = new FileWriter(file));
        } finally {
            IOUtil.closeSilently(writer);
        }
    }

}
