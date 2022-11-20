package io.sniffy.util;

import io.sniffy.log.Polyglog;

// TODO: come up with meaningful toStrings() for all Sniffy objects
// TODO: come up with logging strategy and exception handling (Sniffy should never break applicaiton)
public class AssertUtil {

    public static boolean logAndThrowException(Polyglog polyglog, String message, Throwable throwable) {
        if (isTestingSniffy()) {
            polyglog.error(message, throwable);
            throw ExceptionUtil.throwException(throwable);
        } else {
            return false;
        }
    }

    public static boolean isTestingSniffy() {
        return null != System.getProperty("io.sniffy.test");
    }

}
