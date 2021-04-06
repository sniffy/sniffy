package io.sniffy.tls;

import javax.net.ssl.SSLContextSpi;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.List;
import java.util.Map;

public class SniffySSLContextSpiProviderService extends Provider.Service {

    private final Provider.Service delegate;

    public SniffySSLContextSpiProviderService(Provider provider, String type, String algorithm, String className, List<String> aliases, Map<String, String> attributes, Provider.Service delegate) {
        super(provider, type, algorithm, className, aliases, attributes);
        this.delegate = delegate;
    }


    @Override
    public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
        Object o = delegate.newInstance(constructorParameter);
        if (o instanceof SSLContextSpi) {
            return new SniffySSLContextSpi((SSLContextSpi) o);
        } else {
            return o;
        }
    }

    @Override
    public boolean supportsParameter(Object parameter) {
        return delegate.supportsParameter(parameter);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
