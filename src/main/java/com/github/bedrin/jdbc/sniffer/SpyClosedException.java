package com.github.bedrin.jdbc.sniffer;

import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.generateMessage;
import static com.github.bedrin.jdbc.sniffer.util.StringUtil.LINE_SEPARATOR;

/**
 * @since 2.1
 */
public class SpyClosedException extends IllegalStateException {

    private final StackTraceElement[] closeStackTrace;

    public SpyClosedException(String s, StackTraceElement[] closeStackTrace) {
        super(generateMessage(s + LINE_SEPARATOR + "Close stack trace:", closeStackTrace));
        this.closeStackTrace = closeStackTrace;
    }

    public StackTraceElement[] getCloseStackTrace() {
        return null == closeStackTrace ? null : closeStackTrace.clone();
    }

}
