package io.sniffy.tls;

import io.sniffy.Sniffy;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionUtil;

import javax.net.ssl.*;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.nio.charset.StandardCharsets.US_ASCII;
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

                if (dstLength > 0) {
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
        //return delegate.unwrap(src, dst);

        int position = src.position();
        int srcLength = 0;

        int dstPosition = dst.position();
        int dstLength = 0;

        // The inbound network buffer may be modified as a result of this call: therefore if the network data packet is required for some secondary purpose, the data should be duplicated before calling this method.
        ByteBuffer srcClone = ByteBuffer.allocate(src.limit() - src.position());
        srcClone.put(src);
        src.position(position);
        srcClone.flip();

        System.out.println("dstPosition=" + dstPosition);

        try {
            // TODO: The inbound network buffer may be modified as a result of this call: therefore if the network data packet is required for some secondary purpose, the data should be duplicated before calling this method.
            SSLEngineResult sslEngineResult = delegate.unwrap(src, dst);
            if (handshaking) {
                SSLEngineResult.HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
                SSLEngineResult.Status status = sslEngineResult.getStatus();
                if (NOT_HANDSHAKING == handshakeStatus && OK == status) {
                    handshaking = false;
                }
            }

            if (!handshaking) {
                srcLength = sslEngineResult.bytesConsumed();
                if (srcLength > 0) {
                    dstLength = sslEngineResult.bytesProduced();
                }
            }

            return sslEngineResult;
        } finally {

            if (!handshaking && srcLength > 0) {
                //src.position(position);
                byte[] srcBuff = new byte[srcLength];
                srcClone.get(srcBuff, 0, srcLength);

                System.out.println("Decrypting " + srcLength + " bytes, starting with " + srcBuff[0] + ", " + srcBuff[1] + " and ending with " + srcBuff[srcBuff.length - 2] + ", " + srcBuff[srcBuff.length - 1]);
                System.out.println(Arrays.toString(srcBuff));

                if (dstLength > 0) {
                    dst.position(dstPosition);
                    byte[] dstBuff = new byte[dstLength];
                    dst.get(dstBuff, 0, dstLength);

                    //SocketMetaData socketMetaData = Sniffy.GLOBAL_DECRYPTION_MAP.get(new Sniffy.EncryptedPacket(srcBuff));

                    // TODO: optimize this horrible code below
                    Sniffy.EncryptedPacket encryptedPacket = new Sniffy.EncryptedPacket(srcBuff);
                    SocketMetaData socketMetaData = Sniffy.GLOBAL_DECRYPTION_MAP.get(encryptedPacket);

                    if (null == socketMetaData) {
                        Map.Entry<Sniffy.EncryptedPacket, SocketMetaData> matchedEntry = null;

                        for (Map.Entry<Sniffy.EncryptedPacket, SocketMetaData> entry : Sniffy.GLOBAL_DECRYPTION_MAP.entrySet()) {
                            if (entry.getKey().startsWith(srcBuff)) {
                                matchedEntry = entry;
                            }
                        }

                        if (null != matchedEntry) {

                            Sniffy.EncryptedPacket key = matchedEntry.getKey();
                            socketMetaData = matchedEntry.getValue();

                            Sniffy.GLOBAL_DECRYPTION_MAP.remove(key);
                            Sniffy.GLOBAL_DECRYPTION_MAP.put(key.trimToSize(srcBuff.length), socketMetaData);


                        }
                    } else {
                        Sniffy.GLOBAL_DECRYPTION_MAP.remove(encryptedPacket); // TODO: check that we're doing the same
                    }

                    if (null != socketMetaData) {
                        Sniffy.logDecryptedTraffic(
                                socketMetaData.getConnectionId(),
                                socketMetaData.getAddress(),
                                false,
                                socketMetaData.getProtocol(),
                                dstBuff,
                                0,
                                dstLength,
                                null != socketMetaData.getStackTrace()
                        );
                    }
                }
            }

        }
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
