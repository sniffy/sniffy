package io.sniffy.tls;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import java.security.Provider;

public class SniffySSLContext extends SSLContext {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySSLContext.class);

    public SniffySSLContext(SSLContextSpi contextSpi, Provider provider, String protocol) {
        super(contextSpi, provider, protocol);
        LOG.trace("Created SniffySSLContext(" + contextSpi + ", " + provider + ", " + protocol + ")");
    }

}
