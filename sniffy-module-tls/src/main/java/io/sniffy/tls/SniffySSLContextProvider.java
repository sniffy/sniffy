package io.sniffy.tls;

import io.sniffy.Constants;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;

import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SniffySSLContextProvider extends Provider {

    public static final String SNIFFY_PROVIDER_NAME = "Sniffy";

    public static void install() {

        ProviderList list = Providers.getProviderList();
        ProviderList providerList = ProviderList.insertAt(list, new SniffySSLContextProvider(), 0);
        Providers.setProviderList(providerList);

    }

    public static void uninstall() {

        ProviderList list = Providers.getProviderList();
        Provider sniffyProvider = list.getProvider(SNIFFY_PROVIDER_NAME);
        if (null != sniffyProvider) {
            ProviderList providerList = ProviderList.remove(list, SNIFFY_PROVIDER_NAME);
            Providers.setProviderList(providerList);
        }


    }

    public SniffySSLContextProvider() {
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
