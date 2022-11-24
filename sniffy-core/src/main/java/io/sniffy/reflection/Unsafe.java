package io.sniffy.reflection;

import io.sniffy.reflection.clazz.ClassRef;
import io.sniffy.reflection.clazz.UnresolvedClassRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.ReflectPermission;
import java.security.ProtectionDomain;

/**
 * List of ideas
 * - Convenient way to access sun.misc.Unsafe and other "Unsafe"s
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

    @Deprecated
    public static final int FALLBACK_JAVA_VERSION = 8;

    public static int tryGetJavaVersion() {
        return tryGetJavaVersion(FALLBACK_JAVA_VERSION);
    }

    public static int tryGetJavaVersion(int fallbackJavaVersion) {
        try {
            return getJavaVersion();
        } catch (Exception e) {
            return fallbackJavaVersion;
        }
    }

    public static int getJavaVersion() {
        String version = System.getProperty("java.version");

        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        if (version.contains("-")) {
            version = version.substring(0, version.indexOf("-"));
        }
        return Integer.parseInt(version);
    }

    @SuppressWarnings("RedundantTypeArguments")
    public static @Nonnull RuntimeException throwException(@Nonnull Throwable e) {
        Unsafe.<RuntimeException>throwAny(e);
        return new RuntimeException(e);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(@Nonnull Throwable e) throws E {
        throw (E)e;
    }

    private static class SunMiscUnsafeHolder {

        private final static @Nonnull sun.misc.Unsafe UNSAFE;

        static {
            sun.misc.Unsafe unsafe = null;
            try {
                Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); // TODO: check THE_ONE for Android as well
                f.setAccessible(true);
                unsafe = (sun.misc.Unsafe) f.get(null);
            } catch (Throwable e) {
                e.printStackTrace();
                assert false : e;
            }
            UNSAFE = unsafe;
        }

    }

    // Consider also jdk.internal.misc.Unsafe and jdk.internal.reflect.Unsafe
    public static @Nonnull sun.misc.Unsafe getSunMiscUnsafe() {
        return SunMiscUnsafeHolder.UNSAFE;
    }

    @SuppressWarnings("unused")
    public static void defineSystemClass(@Nonnull @Deprecated String className, @Nonnull byte[] bytes) throws UnsafeException {
        try {
            $(sun.misc.Unsafe.class).
                    getNonStaticMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE, ClassLoader.class, ProtectionDomain.class).
                    invoke(getSunMiscUnsafe(), className, bytes, 0, bytes.length, null, null);
        } catch (Exception e) {
            throw new UnsafeException(e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public static @Nonnull <C> UnresolvedClassRef<C> $(@Nonnull String className) {
        try {
            //noinspection unchecked
            Class<C> clazz = (Class<C>)Class.forName(className);
            return new UnresolvedClassRef<C>(new ClassRef<C>( clazz), null);
        } catch (Throwable e) {
            return new UnresolvedClassRef<C>(null, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <C,C1 extends C> @Nonnull ClassRef<C> $(@Nonnull Class<C1> clazz, @SuppressWarnings("unused") @Nullable Class<C> cast) {
        return (ClassRef<C>) $(clazz);
    }

    public static <C> @Nonnull UnresolvedClassRef<C> $(@Nonnull String className, @SuppressWarnings("unused") @Nullable Class<C> cast) {
        return $(className);
    }

    // TODO: introduce caching
    @SuppressWarnings("Convert2Diamond")
    public static <C> @Nonnull ClassRef<C> $(@Nonnull Class<C> clazz) {
        return new ClassRef<C>(clazz);
    }

    @SuppressWarnings("RedundantSuppression")
    public static boolean setAccessible(@Nonnull AccessibleObject ao) throws UnsafeException {

        //noinspection deprecation
        if (ao.isAccessible()) {
            return true;
        }

        if (tryGetJavaVersion() >= 16) {

            try {
                long overrideOffset = getSunMiscUnsafe().objectFieldOffset(FakeAccessibleObject.class.getDeclaredField("override"));
                getSunMiscUnsafe().putBoolean(ao, overrideOffset, true);
            } catch (NoSuchFieldException e) {
                throw new UnsafeException(e);
            }

            //noinspection deprecation
            return ao.isAccessible();
        }

        ao.setAccessible(true);
        return true;

    }

    /**
     * FakeAccessibleObject class has similar layout as {@link AccessibleObject} and can be used for calculating offsets
     */
    @SuppressWarnings({"unused", "NullableProblems"})
    private static class FakeAccessibleObject implements AnnotatedElement {

        /**
         * The Permission object that is used to check whether a client
         * has sufficient privilege to defeat Java language access
         * control checks.
         */
        static final private java.security.Permission ACCESS_PERMISSION =
                new ReflectPermission("suppressAccessChecks");

        // Indicates whether language-level access checks are overridden
        // by this object. Initializes to "false". This field is used by
        // Field, Method, and Constructor.
        //
        // NOTE: for security purposes, this field must not be visible
        // outside this package.
        boolean override;

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        // Reflection factory used by subclasses for creating field,
        // method, and constructor accessors. Note that this is called
        // very early in the bootstrapping process.
        static final Object reflectionFactory = new Object();

        volatile Object securityCheckCache;

    }

}
