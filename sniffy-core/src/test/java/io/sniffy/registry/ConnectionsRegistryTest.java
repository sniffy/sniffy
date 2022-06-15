package io.sniffy.registry;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import io.qameta.allure.Issue;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.socket.SniffyNetworkConnection;
import org.junit.After;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.Reference;
import java.net.*;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ConnectionsRegistryTest extends BaseSocketTest {

    @After
    public void clearConnectionRules() {
        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void testConnectionClosed() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus(localhost.getHostName(), echoServerRule.getBoundPort(), -1);

        Socket socket = null;

        try {
            socket = new Socket(localhost, echoServerRule.getBoundPort());
            fail("Should have failed since this connection is forbidden by sniffy");
        } catch (ConnectException e) {
            assertNotNull(e);
        } finally {
            if (null != socket) socket.close();
        }

    }

    @Test
    public void testThreadLocalConnectionClosed() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        try {
            ConnectionsRegistry.INSTANCE.setThreadLocal(true);

            HashMap<Map.Entry<String, Integer>, Integer> expected = new HashMap<>();
            expected.put(new AbstractMap.SimpleEntry<>(localhost.getHostName(), echoServerRule.getBoundPort()), -1);
            ConnectionsRegistry.INSTANCE.setThreadLocalDiscoveredAddresses(expected);

            Map<Map.Entry<String, Integer>, Integer> actual = ConnectionsRegistry.INSTANCE.getDiscoveredAddresses();
            assertEquals(expected, actual);

            Socket socket = null;

            try {
                socket = new Socket(localhost, echoServerRule.getBoundPort());
                fail("Should have failed since this connection is forbidden by sniffy");
            } catch (ConnectException e) {
                assertNotNull(e);
            } finally {
                if (null != socket) socket.close();
            }

            AtomicReference<Socket> socketReference = new AtomicReference<>();

            AtomicReference<Exception> exceptionReference = new AtomicReference<>();

            Thread t = new Thread(() -> {
                try {
                    socketReference.set(new Socket(localhost, echoServerRule.getBoundPort()));
                } catch (Exception e) {
                    exceptionReference.set(e);
                }
            });

            t.start();
            t.join(1000);

            assertNull(exceptionReference.get());

            socket = socketReference.get();

            assertNotNull(socket);
            assertTrue(socket.isConnected());

            // Close socket in a separate thread cause closing it from current is prohibited by Sniffy

            try {
                socket.close();
                fail("Should have thrown Exception");
            } catch (SocketException e) {
                assertNotNull(e);
            }

            t = new Thread(() -> {
                try {
                    socketReference.get().close();
                } catch (Exception e) {
                    exceptionReference.set(e);
                }
            });

            t.start();
            t.join(1000);

            assertNull(exceptionReference.get());

        } finally {
            ConnectionsRegistry.INSTANCE.clear();
            ConnectionsRegistry.INSTANCE.setThreadLocal(false);
        }

    }

    @Test
    public void testIsNullConnectionOpened() {

        assertEquals(0, ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(null, null));
        assertEquals(0, ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(new InetSocketAddress((InetAddress) null, 5555), null));
        assertEquals(0, ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(new InetSocketAddress("bad host address", 5555), null));

    }

    @Test
    public void testConnectionOpened() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        Socket socket = new Socket(localhost, echoServerRule.getBoundPort());

        assertTrue(socket.isConnected());

        socket.close();

    }

    @Test
    public void testLoadFromReader() throws Exception {

        String json = "{\"sockets\":[{\"host\":\"google.com\",\"port\":\"42\",\"status\":0}]," +
                "\"dataSources\":[{\"url\":\"jdbc:h2:mem:test\",\"userName\":\"sa\",\"status\":-1}]}";

        ConnectionsRegistry.INSTANCE.readFrom(new StringReader(json));

        Map<Map.Entry<String, Integer>, Integer> discoveredAddresses =
                ConnectionsRegistry.INSTANCE.getDiscoveredAddresses();

        assertEquals(1, discoveredAddresses.size());
        assertEquals(0, discoveredAddresses.get(new AbstractMap.SimpleEntry<>("google.com", 42)).intValue());

        Map<Map.Entry<String, String>, Integer> discoveredDataSources =
                ConnectionsRegistry.INSTANCE.getDiscoveredDataSources();

        assertEquals(1, discoveredDataSources.size());
        assertEquals(-1, discoveredDataSources.get(new AbstractMap.SimpleEntry<>("jdbc:h2:mem:test", "sa")).intValue());

    }

    @Test
    public void testWriteToWriter() throws Exception {

        ConnectionsRegistry.INSTANCE.setDataSourceStatus("dataSource", "userName", 0);
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 6666, -1);

        StringWriter sw = new StringWriter();
        ConnectionsRegistry.INSTANCE.writeTo(sw);

        String persistedConnectionsRegistry = sw.getBuffer().toString();

        assertNotNull(persistedConnectionsRegistry);
    }

    @Test
    @Issue("issues/302")
    public void testSerializeSpecialCharacters() throws Exception {

        ConnectionsRegistry.INSTANCE.clear();

        final String CONNECTION_URL = "jdbc:h2:c:\\work\\keycloak-3.0.0.CR1\\standalone\\data/keycloak;AUTO_SERVER=TRUE";

        ConnectionsRegistry.INSTANCE.setDataSourceStatus(CONNECTION_URL, "sa", 0);

        StringWriter sw = new StringWriter();
        ConnectionsRegistry.INSTANCE.writeTo(sw);

        String persistedConnectionsRegistry = sw.getBuffer().toString();

        assertNotNull(persistedConnectionsRegistry);

        JsonValue connectionRegistryJson = Json.parse(persistedConnectionsRegistry);

        assertEquals(
                CONNECTION_URL,
                connectionRegistryJson.asObject().get("dataSources").asArray().get(0).asObject().get("url").asString()
        );


    }

    @Test
    @Issue("issues/317")
    public void testResolveSocketAddressStatus() throws Exception {

        final AtomicInteger lastConnectionStatus = new AtomicInteger();

        {
            SniffyNetworkConnection sniffyNetworkConnection = new SniffyNetworkConnection() {
                @Override
                public InetSocketAddress getInetSocketAddress() {
                    return null;
                }

                @Override
                public void setConnectionStatus(Integer connectionStatus) {
                    lastConnectionStatus.set(connectionStatus);
                }

                @Override
                public void setProxiedInetSocketAddress(InetSocketAddress proxiedAddress) {

                }

                @Override
                public InetSocketAddress getProxiedInetSocketAddress() {
                    return null;
                }

                @Override
                public void setFirstPacketSent(boolean firstPacketSent) {

                }

                @Override
                public boolean isFirstPacketSent() {
                    return false;
                }

                // TODO: do something with non implemented method below
                @Override
                public int getPotentiallyBufferedInputBytes() {
                    return 0;
                }

                @Override
                public void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes) {

                }

                @Override
                public int getPotentiallyBufferedOutputBytes() {
                    return 0;
                }

                @Override
                public void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes) {

                }

                @Override
                public long getLastReadThreadId() {
                    return 0;
                }

                @Override
                public void setLastReadThreadId(long lastReadThreadId) {

                }

                @Override
                public long getLastWriteThreadId() {
                    return 0;
                }

                @Override
                public void setLastWriteThreadId(long lastWriteThreadId) {

                }

                @Override
                public void logSocket(long millis) {

                }

                @Override
                public void logSocket(long millis, int bytesDown, int bytesUp) {

                }

                @Override
                public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {

                }

                @Override
                public void logDecryptedTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {

                }

                @Override
                public void checkConnectionAllowed() throws ConnectException {

                }

                @Override
                public void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException {

                }

                @Override
                public void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException {

                }

                @Override
                public void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException {

                }
            };

            ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 5555), sniffyNetworkConnection);
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 5555, -42);
        }

        assertEquals(-42, lastConnectionStatus.get());

    }


    @Test
    @Issue("issues/317")
    public void testWeakReferenceQueue() throws Exception {

        ConnectionsRegistry.INSTANCE.sniffySocketImpls.clear();

        InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 5555);

        final AtomicInteger lastConnectionStatus = new AtomicInteger();

        {
            SniffyNetworkConnection sniffyNetworkConnection = new SniffyNetworkConnection() {
                @Override
                public InetSocketAddress getInetSocketAddress() {
                    return inetSocketAddress;
                }

                @Override
                public void setConnectionStatus(Integer connectionStatus) {
                    lastConnectionStatus.set(connectionStatus);
                }

                @Override
                public void setProxiedInetSocketAddress(InetSocketAddress proxiedAddress) {

                }

                @Override
                public InetSocketAddress getProxiedInetSocketAddress() {
                    return null;
                }

                @Override
                public void setFirstPacketSent(boolean firstPacketSent) {

                }

                @Override
                public boolean isFirstPacketSent() {
                    return false;
                }

                // TODO: do something with non implemented method below
                @Override
                public int getPotentiallyBufferedInputBytes() {
                    return 0;
                }

                @Override
                public void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes) {

                }

                @Override
                public int getPotentiallyBufferedOutputBytes() {
                    return 0;
                }

                @Override
                public void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes) {

                }

                @Override
                public long getLastReadThreadId() {
                    return 0;
                }

                @Override
                public void setLastReadThreadId(long lastReadThreadId) {

                }

                @Override
                public long getLastWriteThreadId() {
                    return 0;
                }

                @Override
                public void setLastWriteThreadId(long lastWriteThreadId) {

                }

                @Override
                public void logSocket(long millis) {

                }

                @Override
                public void logSocket(long millis, int bytesDown, int bytesUp) {

                }

                @Override
                public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {

                }

                @Override
                public void logDecryptedTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {

                }

                @Override
                public void checkConnectionAllowed() throws ConnectException {

                }

                @Override
                public void checkConnectionAllowed(int numberOfSleepCycles) throws ConnectException {

                }

                @Override
                public void checkConnectionAllowed(InetSocketAddress inetSocketAddress) throws ConnectException {

                }

                @Override
                public void checkConnectionAllowed(InetSocketAddress inetSocketAddress, int numberOfSleepCycles) throws ConnectException {

                }
            };

            ConnectionsRegistry.INSTANCE.resolveSocketAddressStatus(inetSocketAddress, sniffyNetworkConnection);
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 5555, -42);
        }

        assertEquals(-42, lastConnectionStatus.get());

        ConnectionsRegistry.INSTANCE.sniffySocketImpls.forEach((kve, sniffySocketReferences) -> sniffySocketReferences.forEach(Reference::enqueue));
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("127.0.0.1", 5555, 200);

        // TODO: do the actual assertion about weak reference

    }

}