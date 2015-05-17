package com.github.bedrin.jdbc.sniffer.log;

import org.apache.log4j.Logger;

class Log4JQueryLogger extends QueryLogger {

    protected final static Logger LOG = Logger.getLogger(Log4JQueryLogger.class.getPackage().getName());

    @Override
    protected void log(String message) {
        LOG.debug(message);
    }

    @Override
    protected boolean isVerboseImpl() {
        return LOG.isTraceEnabled();
    }

}
