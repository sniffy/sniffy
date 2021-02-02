package io.sniffy;

import io.sniffy.configuration.SniffyConfiguration;

public class SpyConfiguration {

    private final boolean captureStackTraces;
    private final boolean captureNetwork;
    private final boolean captureNetworkTraffic;
    private final boolean captureJdbc;

    private SpyConfiguration(boolean captureStackTraces, boolean captureNetwork, boolean captureNetworkTraffic, boolean captureJdbc) {
        this.captureStackTraces = captureStackTraces;
        this.captureNetwork = captureNetwork;
        this.captureNetworkTraffic = captureNetworkTraffic;
        this.captureJdbc = captureJdbc;
    }

    public boolean isCaptureStackTraces() {
        return captureStackTraces;
    }

    public boolean isCaptureNetwork() {
        return captureNetwork;
    }

    public boolean isCaptureNetworkTraffic() {
        return captureNetworkTraffic;
    }

    public boolean isCaptureJdbc() {
        return captureJdbc;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean captureStackTraces;
        private boolean captureNetwork;
        private boolean captureNetworkTraffic;
        private boolean captureJdbc;

        public Builder() {
            captureJdbc = SniffyConfiguration.INSTANCE.isMonitorJdbc();
            captureNetwork = SniffyConfiguration.INSTANCE.isMonitorSocket();
            captureStackTraces = true;
        }

        public Builder captureStackTraces(boolean captureStackTraces) {
            this.captureStackTraces = captureStackTraces;
            return this;
        }

        public Builder captureNetwork(boolean captureNetwork) {
            this.captureNetwork = captureNetwork;
            return this;
        }

        public Builder captureNetworkTraffic(boolean captureNetworkTraffic) {
            captureNetwork(captureNetworkTraffic);
            this.captureNetworkTraffic = captureNetworkTraffic;
            return this;
        }

        public Builder captureJdbc(boolean captureJdbc) {
            this.captureJdbc = captureJdbc;
            return this;
        }

        public Builder or(SpyConfiguration spyConfiguration) {
            return captureStackTraces(captureStackTraces || spyConfiguration.captureStackTraces).
                    captureNetwork(captureNetwork || spyConfiguration.captureNetwork).
                    captureNetworkTraffic(captureNetworkTraffic || spyConfiguration.captureNetworkTraffic).
                    captureJdbc(captureJdbc || spyConfiguration.captureJdbc);
        }

        public SpyConfiguration build() {
            return new SpyConfiguration(captureStackTraces, captureNetwork, captureNetworkTraffic, captureJdbc);
        }

    }

}
