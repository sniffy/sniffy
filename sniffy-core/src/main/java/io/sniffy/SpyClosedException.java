package io.sniffy;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.StringUtil;

import static io.sniffy.util.ExceptionUtil.generateMessage;
import static io.sniffy.util.StringUtil.LINE_SEPARATOR;

/**
 * @since 2.1
 */
public class SpyClosedException extends IllegalStateException {

    private final StackTraceElement[] closeStackTrace;

    public SpyClosedException(String s, StackTraceElement[] closeStackTrace) {
        super(ExceptionUtil.generateMessage(s + StringUtil.LINE_SEPARATOR + "Close stack trace:", closeStackTrace));
        this.closeStackTrace = closeStackTrace;
    }

    public StackTraceElement[] getCloseStackTrace() {
        return null == closeStackTrace ? null : closeStackTrace.clone();
    }

}
