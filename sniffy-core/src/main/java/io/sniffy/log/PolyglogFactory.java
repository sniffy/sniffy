package io.sniffy.log;

public class PolyglogFactory {

    public static Polyglog oneTimeLog(Class<?> clazz) {
        return new OneTimePolyglogImpl(log(clazz));
    }

    public static Polyglog log(Class<?> clazz) {
        return new PolyglogSystemOutImpl(clazz);
    }

}
