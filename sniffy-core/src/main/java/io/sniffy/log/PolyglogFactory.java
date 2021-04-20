package io.sniffy.log;

public class PolyglogFactory {

    public static Polyglog log(Class<?> clazz) {
        return new PolyglogSystemOutImpl(clazz);
    }

}
