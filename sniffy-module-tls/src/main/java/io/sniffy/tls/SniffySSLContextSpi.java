package io.sniffy.tls;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionUtil;
import io.sniffy.util.StackTraceExtractor;
import io.sniffy.util.StringUtil;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.SecureRandom;

public class SniffySSLContextSpi extends SSLContextSpi {

    // TODO: cover all methods with unit tests

    private static final Polyglog LOG = PolyglogFactory.log(SniffySSLContextSpi.class);

    private static final Polyglog CONSTRUCTOR_VERBOSE_LOG = PolyglogFactory.oneTimeLog(SniffySSLContextSpi.class);

    private final SSLContextSpi delegate;

    public SniffySSLContextSpi(SSLContextSpi delegate) {
        this.delegate = delegate;
        LOG.trace("Created SniffySSLContextSpi(" + delegate + ")");
        CONSTRUCTOR_VERBOSE_LOG.trace("StackTrace for creating new SniffySSLEngine was " + StringUtil.LINE_SEPARATOR + StackTraceExtractor.getStackTraceAsString());
    }

    @Override
    public void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) throws KeyManagementException {
        try {
            ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineInit", KeyManager[].class, km, TrustManager[].class, tm, SecureRandom.class, sr, Void.class);
        } catch (Exception e) {
            LOG.error(e);
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
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLServerSocketFactory engineGetServerSocketFactory() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetServerSocketFactory", SSLServerSocketFactory.class);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    // TODO: implement SniffySSLEngine and match encrypted payloads with what is put on the wire later
    @Override
    public SSLEngine engineCreateSSLEngine() {
        try {
            return new SniffySSLEngine(
                    ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineCreateSSLEngine", SSLEngine.class)
            );
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    // TODO: implement SniffySSLEngine and match encrypted payloads with what is put on the wire later
    @Override
    public SSLEngine engineCreateSSLEngine(String host, int port) {
        try {
            return new SniffySSLEngine(
                    ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineCreateSSLEngine", String.class, host, Integer.TYPE, port, SSLEngine.class),
                    host, port
            );
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLSessionContext engineGetServerSessionContext() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetServerSessionContext", SSLSessionContext.class);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLSessionContext engineGetClientSessionContext() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetClientSessionContext", SSLSessionContext.class);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLParameters engineGetDefaultSSLParameters() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetDefaultSSLParameters", SSLParameters.class);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLParameters engineGetSupportedSSLParameters() {
        try {
            return ReflectionUtil.invokeMethod(SSLContextSpi.class, delegate, "engineGetSupportedSSLParameters", SSLParameters.class);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

}
