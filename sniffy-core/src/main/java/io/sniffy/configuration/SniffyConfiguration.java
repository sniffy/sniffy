package io.sniffy.configuration;

import io.sniffy.log.PolyglogLevel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import static io.sniffy.log.PolyglogLevel.INFO;

/**
 * @since 3.1
 */
public enum SniffyConfiguration {
    INSTANCE;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * @since 3.1.11
     */
    private volatile PolyglogLevel logLevel;

    private volatile boolean monitorJdbc;
    private volatile boolean monitorSocket;

    /**
     * @since 3.1.7
     */
    private volatile boolean monitorNio;

    /**
     * @since 3.1.11
     */
    private volatile boolean decryptTls;

    /**
     * @since 3.1.10
     */
    @Deprecated
    // TODO: implement
    private volatile boolean captureTraffic;

    /**
     * Enable buffering incoming traffic
     *
     * @since 3.1.13
     */
    private volatile boolean bufferIncomingTraffic;

    /**
     * @since 3.1.2
     */
    private volatile int topSqlCapacity;

    /**
     * Threshold in milliseconds for merging bytes from similar operations when capturing traffic by Sniffy
     *
     * @since 3.1.10
     */
    private volatile int packetMergeThreshold;

    /**
     * If buffering incoming connections is enabled, this property allows to customize incoming buffer size
     *
     * @since 3.1.13
     */
    private volatile int incomingTrafficBufferSize;

    private volatile Boolean filterEnabled;
    private volatile String excludePattern;

    private volatile Boolean injectHtmlEnabled;

    /**
     * @since 3.1.2
     */
    private volatile String injectHtmlExcludePattern;

    // TODO: modify configuration below from @EnableSniffy annotation

    /**
     * @since 3.1.9
     */
    private volatile Boolean jdbcCaptureEnabled; // TODO: implement

    /**
     * @since 3.1.9
     */
    private volatile Boolean jdbcFaultInjectionEnabled; // TODO: implement

    /**
     * Capture stats (bytes and time) on socket IO (including IO, NIO, NIO2/AIO depending on monitorSocket and monitorNio)
     * default - true
     *
     * @since 3.1.9
     */
    private volatile Boolean socketCaptureEnabled;

    /**
     * Enabled fault injection to socket IO  (including IO, NIO, NIO2/AIO depending on monitorSocket and monitorNio)
     * default - true
     *
     * @since 3.1.9
     */
    private volatile Boolean socketFaultInjectionEnabled;

    /**
     * @since 3.1.13
     */
    private volatile Boolean useSecureCookie;

    /**
     * @since 3.1.13
     */
    private volatile Boolean flushResponse;

    /**
     * @since 3.1.13
     */
    private volatile Boolean interceptProxyConnections;

    SniffyConfiguration() {
        loadSniffyConfiguration();
    }

    void loadSniffyConfiguration() {

        String logLevelProperty = getProperty("io.sniffy.logLevel", "IO_SNIFFY_LOG_LEVEL", "info");
        PolyglogLevel polyglogLevel = PolyglogLevel.parse(logLevelProperty);

        logLevel = null == polyglogLevel ? INFO : polyglogLevel;

        monitorJdbc = Boolean.parseBoolean(getProperty(
                "io.sniffy.monitorJdbc", "IO_SNIFFY_MONITOR_JDBC", "true"
        ));
        monitorSocket = Boolean.parseBoolean(getProperty(
                "io.sniffy.monitorSocket", "IO_SNIFFY_MONITOR_SOCKET", "false"
        ));
        monitorNio = Boolean.parseBoolean(getProperty(
                "io.sniffy.monitorNio", "IO_SNIFFY_MONITOR_NIO", "false"
        ));
        decryptTls = Boolean.parseBoolean(getProperty(
                "io.sniffy.decryptTls", "IO_SNIFFY_DECRYPT_TLS", "false"
        ));
        try {
            topSqlCapacity = Integer.parseInt(getProperty(
                    "io.sniffy.topSqlCapacity", "IO_SNIFFY_TOP_SQL_CAPACITY", "1024"
            ));
        } catch (NumberFormatException e) {
            topSqlCapacity = 0;
        }
        try {
            packetMergeThreshold = Integer.parseInt(getProperty(
                    "io.sniffy.packetMergeThreshold", "IO_SNIFFY_PACKET_MERGE_THRESHOLD", "500"
            ));
        } catch (NumberFormatException e) {
            packetMergeThreshold = 0;
        }


        try {
            incomingTrafficBufferSize = Integer.parseInt(getProperty(
                    "io.sniffy.incomingTrafficBufferSize", "IO_SNIFFY_INCOMING_TRAFFIC_BUFFER_SIZE", "51200"
            ));
        } catch (NumberFormatException e) {
            incomingTrafficBufferSize = 0;
        }

        String filterEnabled = getProperty("io.sniffy.filterEnabled", "IO_SNIFFY_FILTER_ENABLED");
        this.filterEnabled = null == filterEnabled ? null : Boolean.parseBoolean(filterEnabled);

        excludePattern = getProperty("io.sniffy.excludePattern", "IO_SNIFFY_EXCLUDE_PATTERN", null);

        String injectHtmlEnabled = getProperty("io.sniffy.injectHtml", "IO_SNIFFY_INJECT_HTML");
        this.injectHtmlEnabled = null == injectHtmlEnabled ? null : Boolean.parseBoolean(injectHtmlEnabled);
        injectHtmlExcludePattern = getProperty("io.sniffy.injectHtmlExcludePattern", "IO_SNIFFY_INJECT_HTML_EXCLUDE_PATTERN", null);

        // TODO: update documentation and tests for new properties below
        String jdbcCaptureEnabled = getProperty("io.sniffy.jdbcCaptureEnabled", "IO_SNIFFY_JDBC_CAPTURE_ENABLED");
        this.jdbcCaptureEnabled = null == jdbcCaptureEnabled || Boolean.parseBoolean(jdbcCaptureEnabled);
        String jdbcFaultInjectionEnabled = getProperty("io.sniffy.jdbcFaultInjectionEnabled", "IO_SNIFFY_JDBC_FAULT_INJECTION_ENABLED");
        this.jdbcFaultInjectionEnabled = null == jdbcFaultInjectionEnabled || Boolean.parseBoolean(jdbcFaultInjectionEnabled);

        String socketCaptureEnabled = getProperty("io.sniffy.socketCaptureEnabled", "IO_SNIFFY_SOCKET_CAPTURE_ENABLED");
        this.socketCaptureEnabled = null == socketCaptureEnabled || Boolean.parseBoolean(socketCaptureEnabled);
        String socketFaultInjectionEnabled = getProperty("io.sniffy.socketFaultInjectionEnabled", "IO_SNIFFY_SOCKET_FAULT_INJECTION_ENABLED");
        this.socketFaultInjectionEnabled = null == socketFaultInjectionEnabled || Boolean.parseBoolean(socketFaultInjectionEnabled);

        String bufferIncomingTraffic = getProperty("io.sniffy.bufferIncomingTraffic", "IO_SNIFFY_BUFFER_INCOMING_TRAFFIC");
        this.bufferIncomingTraffic = Boolean.parseBoolean(bufferIncomingTraffic);

        String useSecureCookie = getProperty("io.sniffy.useSecureCookie", "IO_SNIFFY_USE_SECURE_COOKIE", "true");
        this.useSecureCookie = null == useSecureCookie || Boolean.parseBoolean(useSecureCookie);

        String flushResponse = getProperty("io.sniffy.flushResponse", "IO_SNIFFY_FLUSH_RESPONSE", "true");
        this.flushResponse = null == flushResponse || Boolean.parseBoolean(flushResponse);

        String interceptProxyConnections = getProperty("io.sniffy.interceptProxyConnections", "IO_SNIFFY_INTERCEPT_PROXY_CONNECTIONS", "true");
        this.interceptProxyConnections = null == flushResponse || Boolean.parseBoolean(interceptProxyConnections);

    }

    private String getProperty(String systemPropertyName, String environmentVariableName, String defaultValue) {
        return valueOrDefault(getProperty(systemPropertyName, environmentVariableName), defaultValue);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return null == value ? defaultValue : value;
    }

    private String getProperty(String systemPropertyName, String environmentVariableName) {
        String value = null;

        String env = System.getenv(environmentVariableName);
        if (null != env) {
            value = env;
        }

        String property = System.getProperty(systemPropertyName);
        if (null != property) {
            value = property;
        }

        return value;
    }

    /**
     * @since 3.1.11
     */
    public PolyglogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * @since 3.1.11
     */
    public void setLogLevel(PolyglogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isMonitorJdbc() {
        return monitorJdbc;
    }

    public void setMonitorJdbc(boolean monitorJdbc) {
        this.monitorJdbc = monitorJdbc;
    }

    // monitorSocket

    public boolean isMonitorSocket() {
        return monitorSocket;
    }

    public void setMonitorSocket(boolean monitorSocket) {
        boolean oldValue = this.monitorSocket;
        this.monitorSocket = monitorSocket;
        pcs.firePropertyChange("monitorSocket", oldValue, monitorSocket);
    }

    public boolean isMonitorNio() {
        return monitorNio;
    }

    public void setMonitorNio(boolean monitorNio) {
        boolean oldValue = this.monitorNio;
        this.monitorNio = monitorNio;
        pcs.firePropertyChange("monitorNio", oldValue, monitorNio);
    }

    public boolean isDecryptTls() {
        return decryptTls;
    }

    public void setDecryptTls(boolean decryptTls) {
        boolean oldValue = this.decryptTls;
        this.decryptTls = decryptTls;
        pcs.firePropertyChange("decryptTls", oldValue, decryptTls);
    }

    /**
     * @since 3.1.10
     */
    @Deprecated
    public boolean isCaptureTraffic() {
        return captureTraffic;
    }

    /**
     * @since 3.1.10
     */
    @Deprecated
    public void setCaptureTraffic(boolean captureTraffic) {
        boolean oldValue = this.captureTraffic;
        this.captureTraffic = captureTraffic;
        pcs.firePropertyChange("captureTraffic", oldValue, captureTraffic);
    }

    /**
     * @since 3.1.13
     */
    public boolean isBufferIncomingTraffic() {
        return bufferIncomingTraffic;
    }

    /**
     * @since 3.1.13
     */
    public void setBufferIncomingTraffic(boolean bufferIncomingTraffic) {
        boolean oldValue = this.bufferIncomingTraffic;
        this.bufferIncomingTraffic = bufferIncomingTraffic;
        pcs.firePropertyChange("bufferIncomingTraffic", oldValue, captureTraffic);
    }

    /**
     * @since 3.1.3
     */
    public void addMonitorSocketListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener("monitorSocket", listener);
    }

    /**
     * @since 3.1.3
     */
    public void removeMonitorSocketListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener("monitorSocket", listener);
    }

    /**
     * @since 3.1.7
     */
    public void addMonitorNioListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener("monitorNio", listener);
    }

    /**
     * @since 3.1.7
     */
    public void removeMonitorNioListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener("monitorNio", listener);
    }

    /**
     * @since 3.1.11
     */
    public void addDecryptTlsListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener("decryptTls", listener);
    }

    /**
     * @since 3.1.11
     */
    public void removeDecryptTlsListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener("decryptTls", listener);
    }

    // top sql capacity

    /**
     * @since 3.1.2
     */
    public int getTopSqlCapacity() {
        return topSqlCapacity;
    }

    /**
     * @since 3.1.2
     */
    public void setTopSqlCapacity(int topSqlCapacity) {
        int oldValue = this.topSqlCapacity;
        this.topSqlCapacity = topSqlCapacity;
        pcs.firePropertyChange("topSqlCapacity", oldValue, topSqlCapacity);
    }

    /**
     * @since 3.1.10
     */
    public int getPacketMergeThreshold() {
        return packetMergeThreshold;
    }

    /**
     * @since 3.1.10
     */
    public void setPacketMergeThreshold(int packetMergeThreshold) {
        int oldValue = this.packetMergeThreshold;
        this.packetMergeThreshold = packetMergeThreshold;
        pcs.firePropertyChange("packetMergeThreshold", oldValue, packetMergeThreshold);
    }

    /**
     * @since 3.1.13
     */
    public int getIncomingTrafficBufferSize() {
        return incomingTrafficBufferSize;
    }

    /**
     * @since 3.1.13
     */
    public void setIncomingTrafficBufferSize(int incomingTrafficBufferSize) {
        int oldValue = this.incomingTrafficBufferSize;
        this.incomingTrafficBufferSize = incomingTrafficBufferSize;
        pcs.firePropertyChange("incomingTrafficBufferSize", oldValue, incomingTrafficBufferSize);
    }

    /**
     * @since 3.1.3
     */
    public void addTopSqlCapacityListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener("topSqlCapacity", listener);
    }

    /**
     * @since 3.1.3
     */
    public void removeTopSqlCapacityListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener("topSqlCapacity", listener);
    }

    /**
     * @since 3.1.10
     */
    public void addPacketMergeThresholdListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener("packetMergeThreshold", listener);
    }

    /**
     * @since 3.1.10
     */
    public void removePacketMergeThresholdListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener("packetMergeThreshold", listener);
    }

    // filter enabled

    public Boolean getFilterEnabled() {
        return filterEnabled;
    }

    public void setFilterEnabled(Boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public String getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
    }

    public Boolean getInjectHtmlEnabled() {
        return injectHtmlEnabled;
    }

    public void setInjectHtmlEnabled(Boolean injectHtmlEnabled) {
        this.injectHtmlEnabled = injectHtmlEnabled;
    }

    /**
     * @since 3.1.2
     */
    public String getInjectHtmlExcludePattern() {
        return injectHtmlExcludePattern;
    }

    /**
     * @since 3.1.2
     */
    public void setInjectHtmlExcludePattern(String injectHtmlExcludePattern) {
        this.injectHtmlExcludePattern = injectHtmlExcludePattern;
    }

    /**
     * @since 3.1.9
     */
    public Boolean getJdbcCaptureEnabled() {
        return jdbcCaptureEnabled;
    }

    /**
     * @since 3.1.9
     */
    public void setJdbcCaptureEnabled(Boolean jdbcCaptureEnabled) {
        this.jdbcCaptureEnabled = jdbcCaptureEnabled;
    }

    /**
     * @since 3.1.9
     */
    public Boolean getJdbcFaultInjectionEnabled() {
        return jdbcFaultInjectionEnabled;
    }

    /**
     * @since 3.1.9
     */
    public void setJdbcFaultInjectionEnabled(Boolean jdbcFaultInjectionEnabled) {
        this.jdbcFaultInjectionEnabled = jdbcFaultInjectionEnabled;
    }

    /**
     * @since 3.1.9
     */
    public Boolean getSocketCaptureEnabled() {
        return socketCaptureEnabled;
    }

    /**
     * @since 3.1.9
     */
    public void setSocketCaptureEnabled(Boolean socketCaptureEnabled) {
        this.socketCaptureEnabled = socketCaptureEnabled;
    }

    /**
     * @since 3.1.9
     */
    public Boolean getSocketFaultInjectionEnabled() {
        return socketFaultInjectionEnabled;
    }

    /**
     * @since 3.1.9
     */
    public void setSocketFaultInjectionEnabled(Boolean socketFaultInjectionEnabled) {
        this.socketFaultInjectionEnabled = socketFaultInjectionEnabled;
    }

    /**
     * @since 3.1.13
     */
    public Boolean getUseSecureCookie() {
        return useSecureCookie;
    }

    /**
     * @since 3.1.13
     */
    public Boolean getFlushResponse() {
        return flushResponse;
    }

    /**
     * @since 3.1.13
     */
    public Boolean getInterceptProxyConnections() {
        return interceptProxyConnections;
    }

}
