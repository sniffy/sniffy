package io.sniffy.configuration;

public enum SniffyConfiguration {
    INSTANCE;

    private volatile boolean monitorJdbc;
    private volatile boolean monitorSocket;

    private volatile boolean filterEnabled;
    private volatile boolean injectHtml;
    private volatile String excludePattern;

    SniffyConfiguration() {

        monitorJdbc = Boolean.parseBoolean(getProperty(
                "io.sniffy.monitorJdbc", "IO_SNIFFY_MONITOR_JDBC", "true"
        ));
        monitorSocket = Boolean.parseBoolean(getProperty(
                "io.sniffy.monitorSocket", "IO_SNIFFY_MONITOR_SOCKET", "true"
        ));

        filterEnabled = Boolean.parseBoolean(getProperty(
                "io.sniffy.filterEnabled", "IO_SNIFFY_FILTER_ENABLED", "true"
        ));
        injectHtml = Boolean.parseBoolean(getProperty(
                "io.sniffy.injectHtml", "IO_SNIFFY_INJECT_HTML", "true"
        ));
        excludePattern = getProperty("io.sniffy.excludePattern", "IO_SNIFFY_EXCLUDE_PATTERN", null);

    }

    private String getProperty(String systemPropertyName, String environmentVariableName, String defaultValue) {

        String value = defaultValue;

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
        this.monitorSocket = monitorSocket;
    }

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public void setFilterEnabled(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public boolean isInjectHtml() {
        return injectHtml;
    }

    public void setInjectHtml(boolean injectHtml) {
        this.injectHtml = injectHtml;
    }

    public String getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
    }

}
