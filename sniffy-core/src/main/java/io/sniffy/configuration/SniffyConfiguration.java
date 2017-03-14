package io.sniffy.configuration;

/**
 * @since 3.1
 */
public enum SniffyConfiguration {
    INSTANCE;

    private volatile boolean monitorJdbc;
    private volatile boolean monitorSocket;

    private volatile boolean filterEnabledExplicitly;
    private volatile boolean injectHtmlEnabledExplicitly;

    private volatile boolean filterEnabled;
    private volatile boolean injectHtmlEnabled;
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

        String filterEnabled = getProperty("io.sniffy.filterEnabled", "IO_SNIFFY_FILTER_ENABLED");
        this.filterEnabledExplicitly = Boolean.parseBoolean(filterEnabled);
        this.filterEnabled = Boolean.parseBoolean(valueOrDefault(filterEnabled, "true"));

        String injectHtml = getProperty("io.sniffy.injectHtml", "IO_SNIFFY_INJECT_HTML");
        this.injectHtmlEnabledExplicitly = Boolean.parseBoolean(injectHtml);
        this.injectHtmlEnabled = Boolean.parseBoolean(valueOrDefault(injectHtml, "true"));

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

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public void setFilterEnabled(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public boolean isFilterEnabledExplicitly() {
        return filterEnabledExplicitly;
    }

    public void setFilterEnabledExplicitly(boolean filterEnabledExplicitly) {
        setFilterEnabled(filterEnabledExplicitly);
        this.filterEnabledExplicitly = filterEnabledExplicitly;
    }

    public boolean isInjectHtmlEnabled() {
        return injectHtmlEnabled;
    }

    public void setInjectHtmlEnabled(boolean injectHtmlEnabled) {
        this.injectHtmlEnabled = injectHtmlEnabled;
    }

    public boolean isInjectHtmlEnabledExplicitly() {
        return injectHtmlEnabledExplicitly;
    }

    public void setInjectHtmlEnabledExplicitly(boolean injectHtmlEnabledExplicitly) {
        setInjectHtmlEnabled(injectHtmlEnabledExplicitly);
        this.injectHtmlEnabledExplicitly = injectHtmlEnabledExplicitly;
    }

    public String getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
    }

}
