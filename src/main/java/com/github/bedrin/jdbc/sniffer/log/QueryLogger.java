package com.github.bedrin.jdbc.sniffer.log;

public abstract class QueryLogger {

    private final static QueryLogger INSTANCE;

    static {
        INSTANCE = createQueryLogger();
    }

    static QueryLogger createQueryLogger() {
        try {
            Class.forName("org.slf4j.Logger");
            return new Slf4jQueryLogger();
        } catch (ClassNotFoundException e) {
            return new JavaUtilQueryLogger();
        }
    }

    public static void logQuery(String sql, long nanos) {
        INSTANCE.log(sql + "; -- took " + nanos + " nanos");
    }

    public static boolean isVerbose() {
        return INSTANCE.isVerboseImpl();
    }

    protected abstract void log(String message);

    protected abstract boolean isVerboseImpl();

}
