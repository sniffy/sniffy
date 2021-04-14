package io.sniffy.tls;

import io.sniffy.Sniffy;

import javax.net.ssl.*;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
import static javax.net.ssl.SSLEngineResult.Status.OK;

public class SniffySSLEngine extends SSLEngine {

    private final SSLEngine delegate;

    public SniffySSLEngine(SSLEngine delegate) {
        this.delegate = delegate;
    }

    public SniffySSLEngine(SSLEngine delegate, String peerHost, int peerPort) {
        super(peerHost, peerPort);
        this.delegate = delegate;
    }

    @Override
    public String getPeerHost() {
        return delegate.getPeerHost();
    }

    @Override
    public int getPeerPort() {
        return delegate.getPeerPort();
    }

    private boolean handshaking = false;

    @Override
    public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException {

        int position = src.position();
        int length = 0;

        int dstPosition = dst.position();
        int dstLength = 0;

        System.out.println("dstPosition = " + dstPosition);
        System.out.flush();

        try {
            SSLEngineResult sslEngineResult = delegate.wrap(src, dst);
            if (handshaking) {
                SSLEngineResult.HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
                SSLEngineResult.Status status = sslEngineResult.getStatus();
                if (NOT_HANDSHAKING == handshakeStatus && OK == status) {
                    handshaking = false;
                }
            }

            if (!handshaking) {
                length = sslEngineResult.bytesConsumed();
                if (length > 0) {
                    dstLength = sslEngineResult.bytesProduced();
                }
            }

            return sslEngineResult;
        } finally {

            if (!handshaking && length > 0) {
                src.position(position);
                byte[] buff = new byte[length];
                src.get(buff, 0, length);



                try {
                    System.out.println(new String(buff, "US-ASCII"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (dstLength > 0) {
                    System.out.println("dstLength = " + dstLength);
                    dst.position(dstPosition);
                    byte[] dstBuff = new byte[dstLength];
                    dst.get(dstBuff, 0, dstLength);

                    Sniffy.GLOBAL_ENCRYPTION_MAP.put(
                            new Sniffy.EncryptedPacket(dstBuff),
                            new Sniffy.DecryptedPacket(buff, this)
                    );
                }
            }

        }

    }

    @Override
    public SSLEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws SSLException {
        return delegate.wrap(srcs, dst);
    }

    @Override
    public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
        return delegate.wrap(srcs, offset, length, dst);
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        return delegate.unwrap(src, dst);
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts) throws SSLException {
        return delegate.unwrap(src, dsts);
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
        return delegate.unwrap(src, dsts, offset, length);
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
        handshaking = true;
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
    public String getApplicationProtocol() {
        return delegate.getApplicationProtocol();
    }

    //@Override
    public String getHandshakeApplicationProtocol() {
        return delegate.getHandshakeApplicationProtocol();
    }

    // TODO: wrap methods below on JVMS where it is supported

    public void setHandshakeApplicationProtocolSelector(BiFunction<SSLEngine, List<String>, String> selector) {
        delegate.setHandshakeApplicationProtocolSelector(selector);
    }

    public BiFunction<SSLEngine, List<String>, String> getHandshakeApplicationProtocolSelector() {
        return delegate.getHandshakeApplicationProtocolSelector();
    }

}
