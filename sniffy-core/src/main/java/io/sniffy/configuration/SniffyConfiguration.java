package io.sniffy.configuration;

/**
 * @since 3.1
 */
public enum SniffyConfiguration {
    INSTANCE;

    private volatile boolean monitorJdbc;
    private volatile boolean monitorSocket;

    private volatile int topSqlCapacity;

    private volatile Boolean filterEnabled;
    private volatile Boolean injectHtmlEnabled;
    private volatile String excludePattern;

    SniffyConfiguration() {
        loadSniffyConfiguration();
    }

    void loadSniffyConfiguration() {
        monitorJdbc = Boolean.parseBoolean(getProperty(
                "io.sniffy.monitorJdbc", "IO_SNIFFY_MONITOR_JDBC", "true"
        ));
        monitorSocket = Boolean.parseBoolean(getProperty(
                "io.sniffy.monitorSocket", "IO_SNIFFY_MONITOR_SOCKET", "true"
        ));
        try {
            topSqlCapacity = Integer.parseInt(getProperty(
                    "io.sniffy.topSqlCapacity", "IO_SNIFFY_TOP_SQL_CAPACITY", "1024"
            ));
        } catch (NumberFormatException e) {
            topSqlCapacity = 0;
        }

        String filterEnabled = getProperty("io.sniffy.filterEnabled", "IO_SNIFFY_FILTER_ENABLED");
        this.filterEnabled = null == filterEnabled ? null : Boolean.parseBoolean(filterEnabled);

        String injectHtmlEnabled = getProperty("io.sniffy.injectHtml", "IO_SNIFFY_INJECT_HTML");
        this.injectHtmlEnabled = null == injectHtmlEnabled ? null : Boolean.parseBoolean(injectHtmlEnabled);

        excludePattern = getProperty("io.sniffy.excludePattern", "IO_SNIFFY_EXCLUDE_PATTERN", null);
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

    public boolean isMonitorJdbc() {
        return monitorJdbc;
    }

    public void setMonitorJdbc(boolean monitorJdbc) {
        this.monitorJdbc = monitorJdbc;
    }

    public boolean isMonitorSocket() {
        return monitorSocket;
    }

    public void setMonitorSocket(boolean monitorSocket) {
        // TODO: document why?
        if (!this.monitorSocket) {
            this.monitorSocket = monitorSocket;
        }
    }

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
        this.topSqlCapacity = topSqlCapacity;
    }

    public Boolean getFilterEnabled() {
        return filterEnabled;
    }

    public void setFilterEnabled(Boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public Boolean getInjectHtmlEnabled() {
        return injectHtmlEnabled;
    }

    public void setInjectHtmlEnabled(Boolean injectHtmlEnabled) {
        this.injectHtmlEnabled = injectHtmlEnabled;
    }

    public String getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
    }

}
