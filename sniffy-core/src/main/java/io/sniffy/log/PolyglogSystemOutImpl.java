package io.sniffy.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PolyglogSystemOutImpl extends AbstractPolyglogImpl {

    private final String name;

    public PolyglogSystemOutImpl(String name) {
        this.name = name;
    }

    public PolyglogSystemOutImpl(Class<?> clazz) {
        this(null == clazz ? null : clazz.getSimpleName());
    }

    private final static ThreadLocal<SimpleDateFormat> DATE_FORMAT_THREAD_LOCAL = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        }

    };

    @Override
    public void log(PolyglogLevel level, String message) {
        if (isLevelEnabled(level)) {
            System.out.println(DATE_FORMAT_THREAD_LOCAL.get().format(new Date()) +
                    " [" +
                    level.name() +
                    "]" +
                    " [" +
                    Thread.currentThread().getName() +
                    "] " +
                    ((null == name) ? "" : ("[" + name + "] ")) +
                    message);
        }
    }

    @Override
    public void error(String message, Throwable e) {
        log(PolyglogLevel.ERROR, message);
        error(e);
    }

    @Override
    public void error(Throwable e) {
        if (null != e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            log(PolyglogLevel.ERROR, sw.toString());
        }
    }

}
