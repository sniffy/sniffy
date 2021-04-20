package io.sniffy.log;

public interface Polyglog {

    void trace(String message);

    void debug(String message);

    void info(String message);

    void error(String message);

    void log(PolyglogLevel level, String message);

    void error(String message, Exception e);

    void error(Throwable e);

    boolean isLevelEnabled(PolyglogLevel level);

}
