package io.sniffy.tls;

import io.sniffy.Sniffy;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.socket.SniffySSLNetworkConnection;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.nio.ByteBuffer;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
import static javax.net.ssl.SSLEngineResult.Status.OK;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SniffySSLEngineTrafficTest {

    @Test
    public void gatheringWrapPublishesOnlyPartiallyConsumedPlaintextInBufferOrder() throws Exception {
        final SSLEngine delegate = mock(SSLEngine.class);
        final SniffyNetworkConnection connection = mock(SniffyNetworkConnection.class);
        final ByteBuffer ignoredHead = ascii("ignored-head");
        final ByteBuffer first = ascii("abc");
        final ByteBuffer second = ascii("defg");
        final ByteBuffer ignoredTail = ascii("ignored-tail");
        first.position(1);
        final ByteBuffer[] sources = new ByteBuffer[]{ignoredHead, first, second, ignoredTail};
        final ByteBuffer encrypted = ByteBuffer.allocate(16);
        final byte[] produced = new byte[]{22, 3};

        when(delegate.wrap(same(sources), eq(1), eq(2), same(encrypted)))
                .thenAnswer(new Answer<SSLEngineResult>() {
                    @Override public SSLEngineResult answer(InvocationOnMock invocation) {
                        first.position(first.position() + 1);       // "b"
                        second.position(second.position() + 2);    // "de"
                        encrypted.put(produced);
                        return new SSLEngineResult(OK, NOT_HANDSHAKING, 3, produced.length);
                    }
                });

        SniffySSLEngine engine = new SniffySSLEngine(delegate);
        engine.setSniffyNetworkConnection(connection);
        ByteBuffer cacheKey = ByteBuffer.wrap(produced);
        SniffySSLNetworkConnection previous = Sniffy.CLIENT_HELLO_CACHE.remove(cacheKey);
        try {
            SSLEngineResult result = engine.wrap(sources, 1, 2, encrypted);

            assertEquals(3, result.bytesConsumed());
            assertEquals(2, first.position());
            assertEquals(2, second.position());
            assertEquals(0, ignoredHead.position());
            assertEquals(0, ignoredTail.position());
            assertEquals(produced.length, encrypted.position());
            assertSame(engine, Sniffy.CLIENT_HELLO_CACHE.get(cacheKey));

            ArgumentCaptor<byte[]> plaintext = ArgumentCaptor.forClass(byte[].class);
            verify(connection).logDecryptedTraffic(
                    eq(true), eq(Protocol.TCP), plaintext.capture(), eq(0), eq(3));
            assertArrayEquals(asciiBytes("bde"), plaintext.getValue());
            verifyNoMoreInteractions(connection);
        } finally {
            Sniffy.CLIENT_HELLO_CACHE.remove(cacheKey);
            if (previous != null) Sniffy.CLIENT_HELLO_CACHE.put(cacheKey, previous);
        }
    }

    @Test
    public void scatteringUnwrapPublishesOnlyPartiallyProducedPlaintextInBufferOrder() throws Exception {
        final SSLEngine delegate = mock(SSLEngine.class);
        final SniffyNetworkConnection connection = mock(SniffyNetworkConnection.class);
        final ByteBuffer encrypted = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});
        final ByteBuffer ignoredHead = ByteBuffer.allocate(4);
        final ByteBuffer first = ByteBuffer.allocate(4);
        final ByteBuffer second = ByteBuffer.allocate(4);
        final ByteBuffer ignoredTail = ByteBuffer.allocate(4);
        first.put((byte) '!');
        final ByteBuffer[] destinations = new ByteBuffer[]{ignoredHead, first, second, ignoredTail};

        when(delegate.unwrap(same(encrypted), same(destinations), eq(1), eq(2)))
                .thenAnswer(new Answer<SSLEngineResult>() {
                    @Override public SSLEngineResult answer(InvocationOnMock invocation) {
                        encrypted.position(encrypted.position() + 2);
                        first.put((byte) 'x');
                        second.put((byte) 'y').put((byte) 'z');
                        return new SSLEngineResult(OK, NOT_HANDSHAKING, 2, 3);
                    }
                });

        SniffySSLEngine engine = new SniffySSLEngine(delegate);
        engine.setSniffyNetworkConnection(connection);
        SSLEngineResult result = engine.unwrap(encrypted, destinations, 1, 2);

        assertEquals(3, result.bytesProduced());
        assertEquals(2, encrypted.position());
        assertEquals(2, first.position());
        assertEquals(2, second.position());
        assertEquals(0, ignoredHead.position());
        assertEquals(0, ignoredTail.position());

        ArgumentCaptor<byte[]> plaintext = ArgumentCaptor.forClass(byte[].class);
        verify(connection).logDecryptedTraffic(
                eq(false), eq(Protocol.TCP), plaintext.capture(), eq(0), eq(3));
        assertArrayEquals(asciiBytes("xyz"), plaintext.getValue());
        verifyNoMoreInteractions(connection);
    }

    private static ByteBuffer ascii(String value) {
        return ByteBuffer.wrap(asciiBytes(value));
    }

    private static byte[] asciiBytes(String value) {
        try {
            return value.getBytes("US-ASCII");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
