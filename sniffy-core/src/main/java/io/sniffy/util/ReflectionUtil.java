package io.sniffy.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.locks.Lock;

public class ReflectionUtil {

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

    public static <T, V> boolean setField(Class<T> clazz, T instance, String fieldName, V value, String lockFieldName) {

        //noinspection TryWithIdenticalCatches
        try {
            Field instanceField = clazz.getDeclaredField(fieldName);

            if (!instanceField.isAccessible()) {
                instanceField.setAccessible(true);
            }

            Field modifiersField = getModifiersField();
            modifiersField.setAccessible(true);
            modifiersField.setInt(instanceField, instanceField.getModifiers() & ~Modifier.FINAL);

            if (null != lockFieldName) {

                Field lockField = clazz.getDeclaredField(lockFieldName);

                if (!lockField.isAccessible()) {
                    lockField.setAccessible(true);
                }

                Object lockObject = lockField.get(instance);

                if (lockObject instanceof Lock) {

                    Lock lock = (Lock) lockObject;

                    try {
                        lock.lock();
                        instanceField.set(instance, value);
                        return true;
                    } finally {
                        lock.unlock();
                    }

                } else {

                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (lockObject) {
                        instanceField.set(instance, value);
                        return true;
                    }

                }

            } else {
                instanceField.set(instance, value);
                return true;
            }

        } catch (NoSuchFieldException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
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
        return getField(clazz, instance, fieldName, null);
    }

    public static <T, V> V getField(Class<T> clazz, T instance, String fieldName, String lockFieldName) throws NoSuchFieldException, IllegalAccessException {

        Field instanceField = clazz.getDeclaredField(fieldName);

        if (!instanceField.isAccessible()) {
            instanceField.setAccessible(true);
        }

        Field modifiersField = getModifiersField();
        modifiersField.setAccessible(true);
        modifiersField.setInt(instanceField, instanceField.getModifiers() & ~Modifier.FINAL);

        if (null != lockFieldName) {

            Field lockField = clazz.getDeclaredField(lockFieldName);

            if (!lockField.isAccessible()) {
                lockField.setAccessible(true);
            }

            Object lockObject = lockField.get(instance);

            if (lockObject instanceof Lock) {

                Lock lock = (Lock) lockObject;

                try {
                    lock.lock();
                    //noinspection unchecked
                    return (V) instanceField.get(instance);
                } finally {
                    lock.unlock();
                }

            } else {

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (lockObject) {
                    //noinspection unchecked
                    return (V) instanceField.get(instance);
                }

            }

        } else {
            //noinspection unchecked
            return (V) instanceField.get(instance);
        }

    }

    private static Field getModifiersField() throws NoSuchFieldException {
        try {
            return Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException e) {
            try {
                Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                getDeclaredFields0.setAccessible(true);
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
        Method method = method(clazz, methodName);
        return (R) method.invoke(instance);
    }

    @SuppressWarnings("unchecked")
    public static <R, T, P1> R invokeMethod(
            Class<T> clazz, T instance,
            String methodName,
            Class<P1> argument1Type, P1 argument1,
            @SuppressWarnings("unused") Class<R> returnClass
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = method(clazz, methodName, argument1Type);
        return (R) method.invoke(instance, argument1);
    }

    @SuppressWarnings("unchecked")
    public static <R, T, P1, P2> R invokeMethod(
            Class<T> clazz, T instance,
            String methodName,
            Class<P1> argument1Type, P1 argument1,
            Class<P2> argument2Type, P2 argument2,
            @SuppressWarnings("unused") Class<R> returnClass
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = method(clazz, methodName, argument1Type, argument2Type);
        return (R) method.invoke(instance, argument1, argument2);
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
        Method method = method(clazz, methodName, argument1Type, argument2Type, argument3Type);
        return (R) method.invoke(instance, argument1, argument2, argument3);
    }

    public static Method method(Class<?> clazz, String methodName, Class<?>... argumentTypes) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, argumentTypes);
        method.setAccessible(true);
        return method;
    }

}
