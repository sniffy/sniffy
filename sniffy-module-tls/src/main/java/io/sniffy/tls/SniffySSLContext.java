package io.sniffy.tls;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionUtil;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.SecureRandom;

public class SniffySSLContext extends SSLContextSpi {

    private final SSLContextSpi delegate;

    public SniffySSLContext(SSLContextSpi delegate) {
        this.delegate = delegate;
    }

    @Override
    public void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) throws KeyManagementException {
        try {
            ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineInit", KeyManager[].class, km, TrustManager[].class, tm, SecureRandom.class, sr, Void.class);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLSocketFactory engineGetSocketFactory() {
        try {
            return new SniffySSLSocketFactory(
                    ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetSocketFactory", SSLSocketFactory.class)
            );
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLServerSocketFactory engineGetServerSocketFactory() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetServerSocketFactory", SSLServerSocketFactory.class);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLEngine engineCreateSSLEngine() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineCreateSSLEngine", SSLEngine.class);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLEngine engineCreateSSLEngine(String host, int port) {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineCreateSSLEngine", String.class, host, Integer.TYPE, port, SSLEngine.class);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLSessionContext engineGetServerSessionContext() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetServerSessionContext", SSLSessionContext.class);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLSessionContext engineGetClientSessionContext() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetClientSessionContext", SSLSessionContext.class);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLParameters engineGetDefaultSSLParameters() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetDefaultSSLParameters", SSLParameters.class);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLParameters engineGetSupportedSSLParameters() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetSupportedSSLParameters", SSLParameters.class);
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e);
        }
    }

}
