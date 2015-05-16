package com.github.bedrin.jdbc.sniffer.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Slf4jQueryLogger extends QueryLogger {

    protected final static Logger LOG = LoggerFactory.getLogger(JavaUtilQueryLogger.class.getPackage().getName());

    @Override
    protected void log(String message) {
        LOG.debug(message);
    }

    @Override
    protected boolean isVerboseImpl() {
        return LOG.isTraceEnabled();
    }

}
