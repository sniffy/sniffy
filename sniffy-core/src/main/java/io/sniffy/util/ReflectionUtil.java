package io.sniffy.util;

import sun.misc.Unsafe;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;

public class ReflectionUtil {

    public final static Unsafe UNSAFE;

    private static final int METHOD_MH_ACCESSOR = 0x1;

    static {

        Unsafe unsafe = null;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        UNSAFE = unsafe;

        if (JVMUtil.getVersion() == 18) {

            // workaround https://openjdk.java.net/jeps/416 - JEP 416: Reimplement Core Reflection with Method Handles

            try {

                Class<?> reflectionFactoryClass = Class.forName("jdk.internal.reflect.ReflectionFactory");
                Field useDirectMethodHandle = reflectionFactoryClass.getDeclaredField("useDirectMethodHandle");
                long useDirectMethodHandleOffset = UNSAFE.staticFieldOffset(useDirectMethodHandle);
                UNSAFE.putInt(reflectionFactoryClass, useDirectMethodHandleOffset, METHOD_MH_ACCESSOR);

            } catch (Throwable e) {
                e.printStackTrace();
            }

        } else if (JVMUtil.getVersion() >= 19) {

            // todo: code below seems useless
            try {

                Class<?> reflectionFactoryClass = Class.forName("jdk.internal.reflect.ReflectionFactory");
                Field configField = reflectionFactoryClass.getDeclaredField("config");
                long configOffset = UNSAFE.staticFieldOffset(configField);

                UNSAFE.putObject(reflectionFactoryClass, configOffset, null);
                System.setProperty("jdk.reflect.useDirectMethodHandle", "false");

            } catch (Throwable e) {
                e.printStackTrace();
            }

        }

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

    public static boolean setAccessible(AccessibleObject ao) {

        if (JVMUtil.getVersion() >= 16) {

            try {
                long overrideOffset = UNSAFE.objectFieldOffset(FakeAccessibleObject.class.getDeclaredField("override"));
                UNSAFE.putBoolean(ao, overrideOffset, true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }

            /*if (JVMUtil.getVersion() >= 19 && ao instanceof Field) {
                try {
                    long trustedFinalOffset = UNSAFE.objectFieldOffset(FakeField.class.getDeclaredField("trustedFinal"));
                    UNSAFE.putBoolean(ao, trustedFinalOffset, false);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }*/

            return ao.isAccessible();
        }

        ao.setAccessible(true);
        return true;

    }

    public static <T, V> boolean setField(String className, T instance, String fieldName, V value) {
        return setField(className, instance, fieldName, value, null);
    }

    public static <T, V> boolean setField(String className, T instance, String fieldName, V value, String lockFieldName) {
        try {
            //noinspection unchecked
            return setField((Class<T>) Class.forName(className), instance, fieldName, value, lockFieldName);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static <T, V> boolean setField(Class<T> clazz, T instance, String fieldName, V value) {
        return setField(clazz, instance, fieldName, value, null);
    }

    public static <T, V> boolean setFields(String className, T instance, Class<V> valueClass, V value) {
        try {
            //noinspection unchecked
            return setFields((Class<T>) Class.forName(className), instance, valueClass, value);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static <T, V> boolean setFields(Class<T> clazz, T instance, Class<V> valueClass, V value) {
        boolean fieldsFound = false;
        boolean result = true;
        for (Field field: clazz.getDeclaredFields()) {
            if (field.getType().equals(valueClass)) {
                fieldsFound = true;
                result = result && setField(clazz, instance, field.getName(), value, null);
            }
        }
        return fieldsFound && result;
    }

    public static <T, V> boolean setFirstField(Class<T> clazz, T instance, Class<V> valueClass, V value) {
        for (Field field: clazz.getDeclaredFields()) {
            if (field.getType().equals(valueClass)) {
                return setField(clazz, instance, field.getName(), value, null);
            }
        }
        return false;
    }

    private static final Set<Field> nonAccessibleFields = Collections.<Field>newSetFromMap(
            new ConcurrentHashMap<Field, Boolean>()
    );

    public static <T, V> boolean setField(Class<T> clazz, T instance, String fieldName, V value, String lockFieldName) {

        //noinspection TryWithIdenticalCatches
        try {
            Field instanceField = clazz.getDeclaredField(fieldName);

            /*if (JVMUtil.getVersion() >= 16) {
                long fieldOffset = null == instance ? UNSAFE.staticFieldOffset(instanceField) : UNSAFE.objectFieldOffset(instanceField);
                // TODO: acquire lock
                // TODO: use putvolatile if required
                if (instanceField.getType() == Boolean.TYPE && value instanceof Boolean) {
                    UNSAFE.putBoolean(instance, fieldOffset, (Boolean) value);
                } else if (instanceField.getType() == Integer.TYPE && value instanceof Number) {
                    UNSAFE.putInt(instance, fieldOffset, ((Number) value).intValue());
                } else if (instanceField.getType() == Long.TYPE && value instanceof Number) {
                    UNSAFE.putLong(instance, fieldOffset, ((Number) value).longValue());
                } else if (instanceField.getType() == Short.TYPE && value instanceof Number) {
                    UNSAFE.putShort(instance, fieldOffset, ((Number) value).shortValue());
                } else if (instanceField.getType() == Byte.TYPE && value instanceof Number) {
                    UNSAFE.putByte(instance, fieldOffset, ((Number) value).byteValue());
                } else if (instanceField.getType() == Double.TYPE && value instanceof Number) {
                    UNSAFE.putDouble(instance, fieldOffset, ((Number) value).doubleValue());
                } else if (instanceField.getType() == Float.TYPE && value instanceof Number) {
                    UNSAFE.putFloat(instance, fieldOffset, ((Number) value).floatValue());
                } else if (instanceField.getType() == Character.TYPE && value instanceof Character) {
                    UNSAFE.putChar(instance, fieldOffset, (Character) value);
                }
                UNSAFE.putObject(instance == null ? UNSAFE.staticFieldBase(instanceField) : instance, fieldOffset, value);
                return true;
            }*/

            if (!nonAccessibleFields.contains(instanceField) && !instanceField.isAccessible()) {
                nonAccessibleFields.add(instanceField);
            }

            if (!instanceField.isAccessible()) {
                setAccessible(instanceField);
            }

            Field modifiersField = getModifiersField();
            setAccessible(modifiersField);

            modifiersField.setInt(instanceField, instanceField.getModifiers() & ~Modifier.FINAL);

            // TODO: check if code below can actually work and evaluate it instead of static constructor stuff
            /*if (JVMUtil.getVersion() >= 18) {
                Field trustedFinalField = getTrustedFinalField();
                setAccessible(trustedFinalField);
                trustedFinalField.setBoolean(instanceField, false);
            }*/

            if (null != lockFieldName) {

                Field lockField = clazz.getDeclaredField(lockFieldName);

                if (!lockField.isAccessible()) {
                    setAccessible(lockField);
                }

                Object lockObject = get(lockField, instance);

                if (lockObject instanceof Lock) {

                    Lock lock = (Lock) lockObject;

                    try {
                        lock.lock();
                        set(instanceField, instance, value);
                        return true;
                    } finally {
                        lock.unlock();
                    }

                } else {

                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (lockObject) {
                        set(instanceField, instance, value);
                        return true;
                    }

                }

            } else {
                set(instanceField, instance, value);
                return true;
            }

        } catch (NoSuchFieldException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        }

    }

    protected static <T,V> void set(Field instanceField, T instance, V value) throws IllegalAccessException {
        if (JVMUtil.getVersion() >= 19 && nonAccessibleFields.contains(instanceField)) {

            long offset = null == instance ?
                    UNSAFE.staticFieldOffset(instanceField) :
                    UNSAFE.objectFieldOffset(instanceField);

            Object object = null == instance ? instanceField.getDeclaringClass() : instance;

            if (instanceField.getType() == Boolean.TYPE && value instanceof Boolean) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    UNSAFE.putBooleanVolatile(object, offset, (Boolean) value);
                } else {
                    UNSAFE.putBoolean(object, offset, (Boolean) value);
                }
            } else if (instanceField.getType() == Integer.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    UNSAFE.putIntVolatile(object, offset, ((Number) value).intValue());
                } else {
                    UNSAFE.putInt(object, offset, ((Number) value).intValue());
                }
            } else if (instanceField.getType() == Long.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    UNSAFE.putLongVolatile(object, offset, ((Number) value).longValue());
                } else {
                    UNSAFE.putLong(object, offset, ((Number) value).longValue());
                }
            } else if (instanceField.getType() == Short.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    UNSAFE.putShortVolatile(object, offset, ((Number) value).shortValue());
                } else {
                    UNSAFE.putShort(object, offset, ((Number) value).shortValue());
                }
            } else if (instanceField.getType() == Byte.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    UNSAFE.putByteVolatile(object, offset, ((Number) value).byteValue());
                } else {
                    UNSAFE.putByte(object, offset, ((Number) value).byteValue());
                }
            } else if (instanceField.getType() == Double.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    UNSAFE.putDoubleVolatile(object, offset, ((Number) value).doubleValue());
                } else {
                    UNSAFE.putDouble(object, offset, ((Number) value).doubleValue());
                }
            } else if (instanceField.getType() == Float.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    UNSAFE.putFloatVolatile(object, offset, ((Number) value).floatValue());
                } else {
                    UNSAFE.putFloat(object, offset, ((Number) value).floatValue());
                }
            } else if (instanceField.getType() == Character.TYPE && value instanceof Character) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    UNSAFE.putCharVolatile(object, offset, (Character) value);
                } else {
                    UNSAFE.putChar(object, offset, (Character) value);
                }
            } else {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    UNSAFE.putObjectVolatile(object, offset, value);
                } else {
                    UNSAFE.putObject(object, offset, value);
                }
            }

        } else {
            instanceField.set(instance, value);
        }

    }

    public static <T, V> V getField(String className, T instance, String fieldName) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        return getField(className, instance, fieldName, null);
    }

    public static <T, V> V getField(String className, T instance, String fieldName, String lockFieldName) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        //noinspection unchecked
        return getField((Class<T>) Class.forName(className), instance, fieldName, lockFieldName);
    }

    public static <T, V> V getField(Class<T> clazz, T instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Object field = getField(clazz, instance, fieldName, null);
        //noinspection unchecked
        return (V) field;
    }

    public static <T, V> V getFirstField(String className, T instance, Class<V> valueClass) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        //noinspection unchecked
        return getFirstField((Class<T>) Class.forName(className), instance, valueClass);
    }

    public static <T, V> V getFirstField(Class<T> clazz, T instance, Class<V> valueClass) throws NoSuchFieldException, IllegalAccessException {
        Object resultField = null;
        for (Field field: clazz.getDeclaredFields()) {
            if (field.getType().equals(valueClass)) {
                resultField = getField(clazz, instance, field.getName(), null);
            }
        }
        //noinspection unchecked
        return (V) resultField;
    }

    public static <T, V> V getField(Class<T> clazz, T instance, String fieldName, String lockFieldName) throws NoSuchFieldException, IllegalAccessException {

        Field instanceField = clazz.getDeclaredField(fieldName);

        /*if (JVMUtil.getVersion() >= 16) {
            long fieldOffset = null == instance ? UNSAFE.staticFieldOffset(instanceField) : UNSAFE.objectFieldOffset(instanceField);

            // TODO: acquire lock
            // TODO: use getvolatile if required
            if (instanceField.getType() == Boolean.TYPE) {
                UNSAFE.getBoolean(instance, fieldOffset);
            } else if (instanceField.getType() == Integer.TYPE) {
                UNSAFE.getInt(instance, fieldOffset);
            } else if (instanceField.getType() == Long.TYPE) {
                UNSAFE.getLong(instance, fieldOffset);
            } else if (instanceField.getType() == Short.TYPE) {
                UNSAFE.getShort(instance, fieldOffset);
            } else if (instanceField.getType() == Byte.TYPE) {
                UNSAFE.getByte(instance, fieldOffset);
            } else if (instanceField.getType() == Double.TYPE) {
                UNSAFE.getDouble(instance, fieldOffset);
            } else if (instanceField.getType() == Float.TYPE) {
                UNSAFE.getFloat(instance, fieldOffset);
            } else if (instanceField.getType() == Character.TYPE) {
                UNSAFE.getChar(instance, fieldOffset);
            }

            //noinspection unchecked
            return (V) UNSAFE.getObject(null == instance ? UNSAFE.staticFieldOffset(instanceField) : instance, fieldOffset);

        }*/

        if (!nonAccessibleFields.contains(instanceField) && !instanceField.isAccessible()) {
            nonAccessibleFields.add(instanceField);
        }
        if (!instanceField.isAccessible()) {
            setAccessible(instanceField);
        }

        Field modifiersField = getModifiersField();
        setAccessible(modifiersField);
        modifiersField.setInt(instanceField, instanceField.getModifiers() & ~Modifier.FINAL);

        // TODO: check if we actually need more magic for getters to work or is it only required by setters
        // TODO: check if code below can actually work and evaluate it instead of static constructor stuff
        /*if (JVMUtil.getVersion() >= 18) {
            Field trustedFinalField = getTrustedFinalField();
            setAccessible(trustedFinalField);
            trustedFinalField.setBoolean(instanceField, false);
        }*/

        if (null != lockFieldName) {

            Field lockField = clazz.getDeclaredField(lockFieldName);

            if (!lockField.isAccessible()) {
                lockField.setAccessible(true);
            }

            Object lockObject = get(lockField, instance);

            if (lockObject instanceof Lock) {

                Lock lock = (Lock) lockObject;

                try {
                    lock.lock();
                    //noinspection unchecked
                    return (V) get(instanceField, instance);
                } finally {
                    lock.unlock();
                }

            } else {

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (lockObject) {
                    //noinspection unchecked
                    return (V) get(instanceField, instance);
                }

            }

        } else {
            //noinspection unchecked
            return (V) get(instanceField, instance);
        }

    }

    protected static <T> Object get(Field instanceField, Object instance) throws IllegalAccessException {
        if (JVMUtil.getVersion() >= 19 && nonAccessibleFields.contains(instanceField)) {

            long offset = null == instance ?
                    UNSAFE.staticFieldOffset(instanceField) :
                    UNSAFE.objectFieldOffset(instanceField);

            Object object = null == instance ? instanceField.getDeclaringClass() : instance;

            if (instanceField.getType() == Boolean.TYPE) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    return UNSAFE.getBooleanVolatile(object, offset);
                } else {
                    return UNSAFE.getBoolean(object, offset);
                }
            } else if (instanceField.getType() == Integer.TYPE) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    return UNSAFE.getIntVolatile(object, offset);
                } else {
                    return UNSAFE.getInt(object, offset);
                }
            } else if (instanceField.getType() == Long.TYPE) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    return UNSAFE.getLongVolatile(object, offset);
                } else {
                    return UNSAFE.getLong(object, offset);
                }
            } else if (instanceField.getType() == Short.TYPE) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    return UNSAFE.getShortVolatile(object, offset);
                } else {
                    return UNSAFE.getShort(object, offset);
                }
            } else if (instanceField.getType() == Byte.TYPE) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    return UNSAFE.getByteVolatile(object, offset);
                } else {
                    return UNSAFE.getByte(object, offset);
                }
            } else if (instanceField.getType() == Double.TYPE) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    return UNSAFE.getDoubleVolatile(object, offset);
                } else {
                    return UNSAFE.getDouble(object, offset);
                }
            } else if (instanceField.getType() == Float.TYPE) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    return UNSAFE.getFloatVolatile(object, offset);
                } else {
                    return UNSAFE.getFloat(object, offset);
                }
            } else if (instanceField.getType() == Character.TYPE) {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    return UNSAFE.getCharVolatile(object, offset);
                } else {
                    return UNSAFE.getChar(object, offset);
                }
            } else {
                if (Modifier.isVolatile(instanceField.getModifiers())) {
                    return UNSAFE.getObjectVolatile(object, offset);
                } else {
                    return UNSAFE.getObject(object, offset);
                }
            }

        } else {
            return instanceField.get(instance);
        }
    }

    private static Field getModifiersField() throws NoSuchFieldException {
        try {
            return Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException e) {
            try {
                Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                setAccessible(getDeclaredFields0);
                Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
                for (Field field : fields) {
                    if ("modifiers".equals(field.getName())) {
                        return field;
                    }
                }
            } catch (Exception ex) {
                ExceptionUtil.addSuppressed(e, ex);
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public static <R, T> R invokeMethod(
            Class<T> clazz, T instance,
            String methodName,
            @SuppressWarnings("unused") Class<R> returnClass
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (R) invokeMethod(clazz, instance, methodName);
    }

    public static Object invokeMethod(
            Class<?> clazz, Object instance, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return method(clazz, methodName).invoke(instance);
    }

    @SuppressWarnings("unchecked")
    public static <R, T, P1> R invokeMethod(
            Class<T> clazz, T instance,
            String methodName,
            Class<P1> argument1Type, P1 argument1,
            @SuppressWarnings("unused") Class<R> returnClass
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (R) invokeMethod(clazz, instance, methodName, argument1Type, argument1);
    }

    public static <P1> Object invokeMethod(
            Class<?> clazz, Object instance,
            String methodName,
            Class<P1> argument1Type, Object argument1
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = method(clazz, methodName, argument1Type);
        return method.invoke(instance, argument1);
    }

    @SuppressWarnings("unchecked")
    public static <R, T, P1, P2> R invokeMethod(
            Class<T> clazz, T instance,
            String methodName,
            Class<P1> argument1Type, P1 argument1,
            Class<P2> argument2Type, P2 argument2,
            @SuppressWarnings("unused") Class<R> returnClass
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (R) invokeMethod(clazz, instance, methodName, argument1Type, argument1, argument2Type, argument2);
    }

    public static <P1, P2> Object invokeMethod(
            Class<?> clazz, Object instance,
            String methodName,
            Class<P1> argument1Type, Object argument1,
            Class<P2> argument2Type, Object argument2
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = method(clazz, methodName, argument1Type, argument2Type);
        return method.invoke(instance, argument1, argument2);
    }

    @SuppressWarnings("unchecked")
    public static <R, T, P1, P2, P3> R invokeMethod(
            Class<T> clazz, T instance,
            String methodName,
            Class<P1> argument1Type, P1 argument1,
            Class<P2> argument2Type, P2 argument2,
            Class<P3> argument3Type, P3 argument3,
            @SuppressWarnings("unused") Class<R> returnClass
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (R) invokeMethod(clazz, instance, methodName, argument1Type, argument1, argument2Type, argument2, argument3Type, argument3);
    }

    public static <P1, P2, P3> Object invokeMethod(
            Class<?> clazz, Object instance,
            String methodName,
            Class<P1> argument1Type, Object argument1,
            Class<P2> argument2Type, Object argument2,
            Class<P3> argument3Type, Object argument3
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = method(clazz, methodName, argument1Type, argument2Type, argument3Type);
        return method.invoke(instance, argument1, argument2, argument3);
    }

    public static Method method(Class<?> clazz, String methodName, Class<?>... argumentTypes) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, argumentTypes);
        setAccessible(method);
        return method;
    }

}
