package io.sniffy.log;

public enum PolyglogLevel {
    TRACE,
    DEBUG,
    INFO,
    ERROR,
    OFF;

    public boolean isEnabled(PolyglogLevel enabledLevel) {
        return enabledLevel.ordinal() <= this.ordinal();
    }

    public static PolyglogLevel parse(String value) {

        for (PolyglogLevel level : values()) {
            if (level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }

        return null;

    }

}
