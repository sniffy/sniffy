package com.github.bedrin.jdbc.sniffer.log;

import java.util.logging.Level;
import java.util.logging.Logger;

class JavaUtilQueryLogger extends QueryLogger {

    protected final static Logger LOG = Logger.getLogger(JavaUtilQueryLogger.class.getPackage().getName());

    @Override
    protected void log(String message) {
        LOG.log(Level.FINE, message);
    }

    @Override
    protected boolean isVerboseImpl() {
        return LOG.isLoggable(Level.FINEST);
    }

}
