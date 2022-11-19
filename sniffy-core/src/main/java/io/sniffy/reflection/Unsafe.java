package io.sniffy.reflection;

import java.lang.reflect.Field;

public final class Unsafe {

    private Unsafe() {
    }

    private static class SunMiscUnsafeHolder {

        private final static sun.misc.Unsafe UNSAFE;

        static {
            sun.misc.Unsafe unsafe = null;
            try {
                Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                unsafe = (sun.misc.Unsafe) f.get(null);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            UNSAFE = unsafe;
        }

    }

    public static sun.misc.Unsafe getSunMiscUnsafe() {
        return SunMiscUnsafeHolder.UNSAFE;
    }

    @SuppressWarnings("Convert2Diamond")
    public static <C, T> FieldRef<C,T> $(Class<C> clazz, String fieldName) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            return new FieldRef<C,T>(declaredField);
        } catch (NoSuchFieldException e) {
            return new FieldRef<C,T>(null);
        }
    }

}
