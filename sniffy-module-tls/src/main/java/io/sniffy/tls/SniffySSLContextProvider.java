package io.sniffy.tls;

import io.sniffy.Constants;

import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SniffySSLContextProvider extends Provider {

    public static final String SNIFFY_PROVIDER_NAME = "Sniffy";

    public SniffySSLContextProvider() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchAlgorithmException {
        super(SNIFFY_PROVIDER_NAME, Constants.MAJOR_VERSION, "SniffySSLContextProvider");

        for (Map.Entry<String, Service[]> entry : SniffyProviderListUtil.getOriginalSslContextProviders().entrySet()) {
            String algorithm = entry.getKey();
            for (Service service : entry.getValue()) {
                putService(new SniffySSLContextProviderService(
                        this,
                        SniffyProviderListUtil.SSLCONTEXT,
                        algorithm,
                        SniffySSLContextProviderService.class.getName(), // TODO: is it used? shall we register different names?
                        new ArrayList<String>(),  // TODO: is it used? Shall we pass something?
                        new HashMap<String, String>(),  // TODO: is it used? Shall we pass something?
                        service)
                );
            }
        }

    }

}
