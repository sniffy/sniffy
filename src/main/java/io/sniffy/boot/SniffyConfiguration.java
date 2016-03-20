package io.sniffy.boot;

import io.sniffy.servlet.SnifferFilter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("sniffy")
public class SniffyConfiguration {

    protected final static boolean DEFAULT_ENABLED = true;
    protected final static boolean DEFAULT_INJECT_HTML = true;

    private boolean enabled = DEFAULT_ENABLED;

    private boolean injectHtml = DEFAULT_INJECT_HTML;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInjectHtml() {
        return injectHtml;
    }

    public void setInjectHtml(boolean injectHtml) {
        this.injectHtml = injectHtml;
    }

    @Bean
    public SnifferFilter sniffyFilter() {
        SnifferFilter snifferFilter = new SnifferFilter();
        snifferFilter.setEnabled(enabled);
        snifferFilter.setInjectHtml(injectHtml);
        return snifferFilter;
    }

}