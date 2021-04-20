package io.sniffy.log;

import io.sniffy.configuration.SniffyConfiguration;

public abstract class AbstractPolyglogImpl implements Polyglog {

    @Override
    public void trace(String message) {
        log(PolyglogLevel.TRACE, message);
    }

    @Override
    public void debug(String message) {
        log(PolyglogLevel.DEBUG, message);
    }

    @Override
    public void info(String message) {
        log(PolyglogLevel.INFO, message);
    }

    @Override
    public void error(String message) {
        log(PolyglogLevel.ERROR, message);
    }

    @Override
    public boolean isLevelEnabled(PolyglogLevel level) {
        return level.isEnabled(SniffyConfiguration.INSTANCE.getLogLevel());
    }

}
