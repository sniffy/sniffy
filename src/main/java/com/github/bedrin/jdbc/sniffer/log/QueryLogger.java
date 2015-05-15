package com.github.bedrin.jdbc.sniffer.log;

public abstract class QueryLogger {

    private final static QueryLogger INSTANCE;

    static {
        INSTANCE = new JavaUtilQueryLogger();
    }

    public static void logQuery(String sql, long nanos) {
        INSTANCE.log(sql);
    }

    protected abstract void log(String message);

}
