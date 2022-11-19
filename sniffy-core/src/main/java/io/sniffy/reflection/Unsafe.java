package io.sniffy.reflection;

import java.lang.reflect.Field;

/**
 * List of ideas
 * - Convenient way to access sun.misc.Unsafe
 * - Safe wrapper (or multiple implementations) around sun.misc.Unsafe
 * - Reflection library based on Unsafe (or on safe wrapper above)
 * - Tooling for attaching agent to self
 * - Tooling for disabling Jigsaw
 * - Tooling for obtaining Instrumentation instance
 * - SizeOf
 * - Esoteric stuff (invoke constructor again, etc.)
 * - Whatever is required by other tools like caches (sizeof), mocks (power reflection), etc.
 */
public final class Unsafe {

    private Unsafe() {
    }

    private static class SunMiscUnsafeHolder {

        private final static sun.misc.Unsafe UNSAFE;

        static {
            sun.misc.Unsafe unsafe = null;
            try {
                Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); // TODO: check THE_ONE for Android as well
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
    public static <C, T> FieldRef<C,T> $(String className, String fieldName) {
        try {
            //noinspection unchecked
            Class<C> clazz = (Class<C>) Class.forName(className);
            return $(clazz, fieldName);
        } catch (Throwable e) {
            return new FieldRef<C,T>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public static <C, T> FieldRef<C,T> $(Class<C> clazz, String fieldName) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            return new FieldRef<C,T>(declaredField, null);
        } catch (Throwable e) {
            return new FieldRef<C,T>(null, e);
        }
    }

}
