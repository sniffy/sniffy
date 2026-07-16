package io.sniffy.registry;

import io.sniffy.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @since 3.1
 */
public enum ConnectionsRegistryStorage {
    INSTANCE;

    private File file = new File(IOUtil.getApplicationSniffyFolder(), "connectionsRegistry.json");

    public void loadConnectionsRegistry(ConnectionsRegistry connectionsRegistry) throws IOException {

        FileReader reader = null;
        try {
            if (file.exists()) {
                connectionsRegistry.readFrom(reader = new FileReader(file));
            }
        } finally {
            IOUtil.closeSilently(reader);
            file.delete();
        }
    }

    public void storeConnectionsRegistry(ConnectionsRegistry connectionsRegistry) throws IOException {

        if (ConnectionsRegistry.INSTANCE.isThreadLocal()) return;

        FileWriter writer = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            connectionsRegistry.writeTo(writer = new FileWriter(file));
        } finally {
            IOUtil.closeSilently(writer);
        }
    }


    StorageSnapshot takeStorageSnapshot() {
        if (!file.exists()) {
            return new StorageSnapshot(false, null);
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return new StorageSnapshot(true, outputStream.toByteArray());
        } catch (IOException e) {
            return new StorageSnapshot(false, null, e);
        } finally {
            IOUtil.closeSilently(inputStream);
        }
    }

    void restoreStorageSnapshot(StorageSnapshot snapshot) throws IOException {
        if (snapshot == null) return;
        if (snapshot.failure != null) throw snapshot.failure;
        if (!snapshot.exists) {
            if (file.exists() && !file.delete()) {
                throw new IOException("Could not delete " + file);
            }
            return;
        }
        FileOutputStream outputStream = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file);
            outputStream.write(snapshot.content);
        } finally {
            IOUtil.closeSilently(outputStream);
        }
    }

    static final class StorageSnapshot {
        private final boolean exists;
        private final byte[] content;
        private final IOException failure;

        private StorageSnapshot(boolean exists, byte[] content) {
            this(exists, content, null);
        }

        private StorageSnapshot(boolean exists, byte[] content, IOException failure) {
            this.exists = exists;
            this.content = content;
            this.failure = failure;
        }
    }
}
