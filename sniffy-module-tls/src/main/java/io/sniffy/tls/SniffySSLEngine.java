package io.sniffy.tls;

import io.sniffy.Sniffy;
import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SniffyNetworkConnection;
import io.sniffy.socket.SniffySSLNetworkConnection;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionUtil;
import io.sniffy.util.StackTraceExtractor;
import io.sniffy.util.StringUtil;

import javax.net.ssl.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.BiFunction;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
import static javax.net.ssl.SSLEngineResult.Status.OK;

public class SniffySSLEngine extends SSLEngine implements SniffySSLNetworkConnection {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySSLSocketFactory.class);

    private static final Polyglog CONSTRUCTOR_VERBOSE_LOG = PolyglogFactory.oneTimeLog(SniffySSLSocketFactory.class);

    private static final Polyglog WRAP_VERBOSE_LOG = PolyglogFactory.oneTimeLog(SniffySSLSocketFactory.class);

    private static final Polyglog UNKNOWN_CONNECTION_LOG = PolyglogFactory.oneTimeLog(SniffySSLSocketFactory.class);

    private final SSLEngine delegate;

    public SniffySSLEngine(SSLEngine delegate) {
        this.delegate = delegate;
        LOG.trace("Created SniffySSLEngine(" + delegate + ")");
        CONSTRUCTOR_VERBOSE_LOG.trace("StackTrace for creating new SniffySSLEngine was " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
    }

    public SniffySSLEngine(SSLEngine delegate, String peerHost, int peerPort) {
        super(peerHost, peerPort);
        this.delegate = delegate;
        LOG.trace("Created SniffySSLEngine(" + delegate + ", " + peerHost + ", " + peerPort + ")");
        CONSTRUCTOR_VERBOSE_LOG.trace("StackTrace for creating new SniffySSLEngine was " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
    }

    @Override
    public String getPeerHost() {
        return delegate.getPeerHost();
    }

    @Override
    public int getPeerPort() {
        return delegate.getPeerPort();
    }

    private volatile SniffyNetworkConnection sniffyNetworkConnection;

    private boolean firstWrap = true;

    @Override
    public SniffyNetworkConnection getSniffyNetworkConnection() {
        return sniffyNetworkConnection;
    }

    @Override
    public void setSniffyNetworkConnection(SniffyNetworkConnection sniffyNetworkConnection) {
        this.sniffyNetworkConnection = sniffyNetworkConnection;
    }

    @Override
    public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException {

        WRAP_VERBOSE_LOG.trace("StackTrace for first SSLEngine.wrap() invocation was " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());

        int srcPosition = src.position();
        int srcLength = 0;

        int dstPosition = dst.position();
        int dstLength = 0;

        boolean handshaking = false;

        try {
            SSLEngineResult sslEngineResult = delegate.wrap(src, dst);

            SSLEngineResult.HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
            SSLEngineResult.Status status = sslEngineResult.getStatus();
            handshaking = NOT_HANDSHAKING != handshakeStatus || OK != status;

            srcLength = sslEngineResult.bytesConsumed();
            dstLength = sslEngineResult.bytesProduced();

            return sslEngineResult;
        } finally {

            if (firstWrap && dstLength > 0) {
                firstWrap = false;

                byte[] dstBuff = copyBytes(dst, dstPosition, dstLength); // TODO: limit it if it's bigger than say 512

                Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(dstBuff), this);

            }

            if (!handshaking && srcLength > 0 && dstLength > 0) {

                if (null == sniffyNetworkConnection) {
                    UNKNOWN_CONNECTION_LOG.trace("SSLEngine invoked for unknown connection id " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
                } else {

                    byte[] buff = copyBytes(src, srcPosition, srcLength);

                    sniffyNetworkConnection.logDecryptedTraffic(
                            true,
                            Protocol.TCP,
                            buff, 0, srcLength
                    );

                }

            }

        }

    }

    @Override
    public SSLEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws SSLException {
        return wrap(srcs, 0, null == srcs ? 0 : srcs.length, dst);
    }

    @Override
    public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
        WRAP_VERBOSE_LOG.trace("StackTrace for first SSLEngine.wrap() invocation was " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());

        int srcLength = 0;

        int[] positions = new int[length];
        for (int i = 0; i < length; i++) {
            positions[i] = srcs[offset + i].position();
        }

        int dstPosition = dst.position();
        int dstLength = 0;

        boolean handshaking = false;

        try {
            SSLEngineResult sslEngineResult = delegate.wrap(srcs, offset, length, dst);

            SSLEngineResult.HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
            SSLEngineResult.Status status = sslEngineResult.getStatus();
            handshaking = NOT_HANDSHAKING != handshakeStatus || OK != status;

            srcLength = sslEngineResult.bytesConsumed();
            dstLength = sslEngineResult.bytesProduced();

            return sslEngineResult;
        } finally {

            if (firstWrap && dstLength > 0) {
                firstWrap = false;

                byte[] dstBuff = copyBytes(dst, dstPosition, dstLength); // TODO: limit it if it's bigger than say 512

                Sniffy.CLIENT_HELLO_CACHE.put(ByteBuffer.wrap(dstBuff), this);

            }

            if (!handshaking && srcLength > 0 && dstLength > 0) {

                if (null == sniffyNetworkConnection) {
                    UNKNOWN_CONNECTION_LOG.trace("SSLEngine invoked for unknown connection id " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
                } else {
                    byte[] buff = copyTransferredBytes(srcs, offset, length, positions, srcLength);
                    if (buff.length > 0) {
                        sniffyNetworkConnection.logDecryptedTraffic(
                                true,
                                Protocol.TCP,
                                buff, 0, buff.length
                        );
                    }
                }

            }

        }

    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException {

        int srcLength = 0;

        int dstPosition = dst.position();
        int dstLength = 0;

        boolean handshaking = false;

        try {
            SSLEngineResult sslEngineResult = delegate.unwrap(src, dst);

            SSLEngineResult.HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
            SSLEngineResult.Status status = sslEngineResult.getStatus();
            handshaking = NOT_HANDSHAKING != handshakeStatus || OK != status;

            srcLength = sslEngineResult.bytesConsumed();
            dstLength = sslEngineResult.bytesProduced();

            return sslEngineResult;
        } finally {

            if (!handshaking && srcLength > 0 && dstLength > 0) {

                if (null == sniffyNetworkConnection) {
                    UNKNOWN_CONNECTION_LOG.trace("SSLEngine invoked for unknown connection id " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
                } else {
                    byte[] buff = copyBytes(dst, dstPosition, dstLength);

                    sniffyNetworkConnection.logDecryptedTraffic(
                            false,
                            Protocol.TCP,
                            buff, 0, dstLength
                    );
                }

            }

        }

    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts) throws SSLException {
        return unwrap(src, dsts, 0, null == dsts ? 0 : dsts.length);
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
        LOG.trace("Scattering unwrap to " + length + " ByteBuffer instances");

        int srcLength = 0;

        int[] positions = new int[length];
        for (int i = 0; i < length; i++) {
            positions[i] = dsts[offset + i].position();
        }

        int dstLength = 0;

        boolean handshaking = false;

        try {
            SSLEngineResult sslEngineResult = delegate.unwrap(src, dsts, offset, length);

            SSLEngineResult.HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
            SSLEngineResult.Status status = sslEngineResult.getStatus();
            handshaking = NOT_HANDSHAKING != handshakeStatus || OK != status;

            srcLength = sslEngineResult.bytesConsumed();
            dstLength = sslEngineResult.bytesProduced();

            return sslEngineResult;
        } finally {

            if (!handshaking && srcLength > 0 && dstLength > 0) {

                if (null == sniffyNetworkConnection) {
                    UNKNOWN_CONNECTION_LOG.trace("SSLEngine invoked for unknown connection id " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
                } else {
                    byte[] buff = copyTransferredBytes(dsts, offset, length, positions, dstLength);
                    if (buff.length > 0) {
                        sniffyNetworkConnection.logDecryptedTraffic(
                                false,
                                Protocol.TCP,
                                buff, 0, buff.length
                        );
                    }
                }

            }

        }

    }

    private static byte[] copyBytes(ByteBuffer buffer, int position, int length) {
        ByteBuffer duplicate = buffer.duplicate();
        limit(duplicate, position + length);
        position(duplicate, position);
        byte[] bytes = new byte[length];
        duplicate.get(bytes);
        return bytes;
    }

    /**
     * Copies at most the aggregate byte count reported by SSLEngine, using each
     * buffer's actual position advance to preserve buffer order and partial-transfer
     * boundaries without changing application-visible positions or limits.
     */
    private static byte[] copyTransferredBytes(ByteBuffer[] buffers, int offset, int length,
                                               int[] initialPositions, int reportedBytes) {
        byte[] bytes = new byte[reportedBytes];
        int destinationOffset = 0;
        for (int i = 0; i < length && destinationOffset < reportedBytes; i++) {
            ByteBuffer buffer = buffers[offset + i];
            int transferred = Math.min(
                    buffer.position() - initialPositions[i], reportedBytes - destinationOffset);
            if (transferred > 0) {
                ByteBuffer duplicate = buffer.duplicate();
                limit(duplicate, initialPositions[i] + transferred);
                position(duplicate, initialPositions[i]);
                duplicate.get(bytes, destinationOffset, transferred);
                destinationOffset += transferred;
            }
        }
        if (destinationOffset == reportedBytes) return bytes;

        // A compliant SSLEngine advances buffer positions by exactly the reported
        // aggregate. If a provider violates that contract, publish only bytes that
        // can be observed safely instead of padding or reading beyond a buffer.
        byte[] exactBytes = new byte[destinationOffset];
        System.arraycopy(bytes, 0, exactBytes, 0, destinationOffset);
        return exactBytes;
    }

    private static void position(ByteBuffer buffer, int position) {
        ((Buffer) buffer).position(position);
    }

    private static void limit(ByteBuffer buffer, int limit) {
        ((Buffer) buffer).limit(limit);
    }

    @Override
    public Runnable getDelegatedTask() {
        return delegate.getDelegatedTask();
    }

    @Override
    public void closeInbound() throws SSLException {
        delegate.closeInbound();
    }

    @Override
    public boolean isInboundDone() {
        return delegate.isInboundDone();
    }

    @Override
    public void closeOutbound() {
        delegate.closeOutbound();
    }

    @Override
    public boolean isOutboundDone() {
        return delegate.isOutboundDone();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return delegate.getEnabledCipherSuites();
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        delegate.setEnabledCipherSuites(suites);
    }

    @Override
    public String[] getSupportedProtocols() {
        return delegate.getSupportedProtocols();
    }

    @Override
    public String[] getEnabledProtocols() {
        return delegate.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        delegate.setEnabledProtocols(protocols);
    }

    @Override
    public SSLSession getSession() {
        return delegate.getSession();
    }

    @Override
    public SSLSession getHandshakeSession() {
        return delegate.getHandshakeSession();
    }

    @Override
    public void beginHandshake() throws SSLException {
        delegate.beginHandshake();
    }

    @Override
    public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
        return delegate.getHandshakeStatus();
    }

    @Override
    public void setUseClientMode(boolean mode) {
        delegate.setUseClientMode(mode);
    }

    @Override
    public boolean getUseClientMode() {
        return delegate.getUseClientMode();
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        delegate.setNeedClientAuth(need);
    }

    @Override
    public boolean getNeedClientAuth() {
        return delegate.getNeedClientAuth();
    }

    @Override
    public void setWantClientAuth(boolean want) {
        delegate.setWantClientAuth(want);
    }

    @Override
    public boolean getWantClientAuth() {
        return delegate.getWantClientAuth();
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        delegate.setEnableSessionCreation(flag);
    }

    @Override
    public boolean getEnableSessionCreation() {
        return delegate.getEnableSessionCreation();
    }

    @Override
    public SSLParameters getSSLParameters() {
        return delegate.getSSLParameters();
    }

    @Override
    public void setSSLParameters(SSLParameters params) {
        delegate.setSSLParameters(params);
    }

    //@Override
    @SuppressWarnings("TryWithIdenticalCatches")
    public String getApplicationProtocol() {
        try {
            return ReflectionUtil.invokeMethod(SSLEngine.class, delegate, "getApplicationProtocol", String.class);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.throwException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    //@Override
    @SuppressWarnings("TryWithIdenticalCatches")
    public String getHandshakeApplicationProtocol() {
        try {
            return ReflectionUtil.invokeMethod(SSLEngine.class, delegate, "getHandshakeApplicationProtocol", String.class);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.throwException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    // TODO: wrap methods below on JVMS where it is supported
    @SuppressWarnings("TryWithIdenticalCatches")
    public void setHandshakeApplicationProtocolSelector(BiFunction<SSLEngine, List<String>, String> selector) {
        try {
            ReflectionUtil.invokeMethod(SSLEngine.class, delegate, "setHandshakeApplicationProtocolSelector", BiFunction.class, selector, Void.class);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.throwException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    @SuppressWarnings({"unchecked", "TryWithIdenticalCatches"})
    public BiFunction<SSLEngine, List<String>, String> getHandshakeApplicationProtocolSelector() {
        try {
            return (BiFunction<SSLEngine, List<String>, String>)
                    ReflectionUtil.invokeMethod(SSLEngine.class, delegate, "getHandshakeApplicationProtocolSelector", BiFunction.class);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.throwException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.throwException(e);
        }
    }

}
