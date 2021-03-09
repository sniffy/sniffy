package io.sniffy.tls;

public class SniffyTlsModule {

    public static void initialize() {

        SniffySSLContextProvider.install();

        // TODO: uncomment below probably with a feature flag
        /*try {
            SniffyProviderListUtil.wrapSSLContextServiceProvidersWithSniffy();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

    }

}
