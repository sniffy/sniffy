package io.sniffy.tls;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.StackTraceExtractor;
import io.sniffy.util.StringUtil;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.SecureRandom;

import static io.sniffy.reflection.Unsafe.$;

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
            $(SSLContextSpi.class).method("engineInit", KeyManager[].class, TrustManager[].class, SecureRandom.class).invoke(delegate, km, tm,sr);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLSocketFactory engineGetSocketFactory() {
        try {
            return new SniffySSLSocketFactory(
                    $(SSLContextSpi.class).method(SSLSocketFactory.class, "engineGetSocketFactory").invoke(delegate)
            );
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLServerSocketFactory engineGetServerSocketFactory() {
        try {
            return $(SSLContextSpi.class).method(SSLServerSocketFactory.class, "engineGetServerSocketFactory").invoke(delegate);
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
                $(SSLContextSpi.class).method(SSLEngine.class, "engineCreateSSLEngine").invoke(delegate)
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
                    $(SSLContextSpi.class).method(SSLEngine.class, "engineCreateSSLEngine", String.class, Integer.TYPE).invoke(delegate, host, port),
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
            return $(SSLContextSpi.class).method(SSLSessionContext.class, "engineGetServerSessionContext").invoke(delegate);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLSessionContext engineGetClientSessionContext() {
        try {
            return $(SSLContextSpi.class).method(SSLSessionContext.class, "engineGetClientSessionContext").invoke(delegate);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLParameters engineGetDefaultSSLParameters() {
        try {
            return $(SSLContextSpi.class).method(SSLParameters.class, "engineGetDefaultSSLParameters").invoke(delegate);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SSLParameters engineGetSupportedSSLParameters() {
        try {
            return $(SSLContextSpi.class).method(SSLParameters.class, "engineGetSupportedSSLParameters").invoke(delegate);
        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.throwException(e);
        }
    }

}
