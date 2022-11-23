package io.sniffy.reflection.field;

import io.sniffy.reflection.Unsafe;
import io.sniffy.reflection.UnsafeInvocationException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class StaticFieldRef<T> {

    private final Field field;

    public StaticFieldRef(Field field) {
        this.field = field;
    }

    public Field getField() {
        return field;
    }

    public boolean compareAndSet(T oldValue, T newValue) throws UnsafeInvocationException {
        try {

            sun.misc.Unsafe UNSAFE = Unsafe.getSunMiscUnsafe();

            /*
            Ensure the given class has been initialized. This is often needed in conjunction with obtaining the static field base of a class.
             */
            UNSAFE.ensureClassInitialized(field.getDeclaringClass());

            long offset = UNSAFE.staticFieldOffset(field);

            Object object = field.getDeclaringClass();

            // TODO: validate conversion below; use new Unsafe on modern JDK

            if (field.getType() == Integer.TYPE && oldValue instanceof Number && newValue instanceof Number) {
                return UNSAFE.compareAndSwapInt(object, offset, ((Number) oldValue).intValue(), ((Number) newValue).intValue());
            } else if (field.getType() == Short.TYPE && oldValue instanceof Number && newValue instanceof Number) {
                // TODO: cover with tests
                return UNSAFE.compareAndSwapInt(object, offset, ((Number) oldValue).intValue(), ((Number) newValue).intValue());
            } else if (field.getType() == Byte.TYPE && oldValue instanceof Number && newValue instanceof Number) {
                // TODO: cover with tests
                return UNSAFE.compareAndSwapInt(object, offset, ((Number) oldValue).intValue(), ((Number) newValue).intValue());
            } else if (field.getType() == Character.TYPE && oldValue instanceof Character && newValue instanceof Character) {
                // TODO: cover with tests
                return UNSAFE.compareAndSwapInt(object, offset, (int) (Character) oldValue, (int) (Character) newValue);
            } else if (field.getType() == Float.TYPE && oldValue instanceof Number && newValue instanceof Number) {
                // TODO: cover with tests
                return UNSAFE.compareAndSwapInt(object, offset, Float.floatToIntBits(((Number) oldValue).floatValue()), Float.floatToIntBits(((Number) newValue).floatValue()));
            } else if (field.getType() == Long.TYPE && oldValue instanceof Number && newValue instanceof Number) {
                return UNSAFE.compareAndSwapLong(object, offset, ((Number) oldValue).longValue(), ((Number) newValue).longValue());
            } else if (field.getType() == Double.TYPE && oldValue instanceof Number && newValue instanceof Number) {
                // TODO: cover with tests
                return UNSAFE.compareAndSwapLong(object, offset, Double.doubleToLongBits(((Number) oldValue).doubleValue()), Double.doubleToLongBits(((Number) newValue).doubleValue()));
            } else if (field.getType() == Boolean.TYPE && oldValue instanceof Boolean && newValue instanceof Boolean) {
                // TODO: implement proper CAS
                boolean currentValue = UNSAFE.getBooleanVolatile(object, offset);
                if (currentValue == (Boolean) oldValue) {
                    UNSAFE.putBooleanVolatile(object, offset, (Boolean) newValue);
                    return true;
                } else {
                    return false;
                }
                // return UNSAFE.compareAndSwapInt(object, offset, (Boolean) oldValue ? 1 : 0, (Boolean) newValue ? 1 : 0);
            } else {
                return UNSAFE.compareAndSwapObject(object, offset, oldValue, newValue);
            }

        } catch (Throwable e) {
            throw new UnsafeInvocationException(e);
        }

    }

    /*private static Field getModifiersField() throws UnsafeException {
        try {
            return Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException e) {
            try {
                Field[] fields = getDeclaredFields(Field.class);
                for (Field field : fields) {
                    if ("modifiers".equals(field.getName())) {
                        return field;
                    }
                }
            } catch (Exception ex) {
                ExceptionUtil.addSuppressed(e, ex);
            }
            throw new UnsafeException(e);
        }
    }

    public static Field[] getDeclaredFields(Class<?> clazz) throws UnsafeException {
        try {
            Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
            Unsafe.setAccessible(getDeclaredFields0);
            return (Field[]) getDeclaredFields0.invoke(clazz, false);
        } catch (Exception e) {
            throw new UnsafeException(e);
        }
    }

    public void removeFinalFlag() throws UnsafeException {

        Field modifiersField = getModifiersField();
        Unsafe.setAccessible(modifiersField);

        try {
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (IllegalAccessException e) {
            throw new UnsafeException();
        }
    }*/

    public void set(T value) throws UnsafeInvocationException {
        try {

            /*if (!field.isAccessible()) {
                Unsafe.setAccessible(field);
            }

            if (Modifier.isFinal(field.getModifiers())) {
                removeFinalFlag();
            }

            if (field.isAccessible()) {
                field.set(instance, value);
                return;
            }*/

            sun.misc.Unsafe UNSAFE = Unsafe.getSunMiscUnsafe();

            /*
            Ensure the given class has been initialized. This is often needed in conjunction with obtaining the static field base of a class.
             */
            UNSAFE.ensureClassInitialized(field.getDeclaringClass());

            long offset = UNSAFE.staticFieldOffset(field);

            Object object = field.getDeclaringClass();

            if (field.getType() == Boolean.TYPE && value instanceof Boolean) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    UNSAFE.putBooleanVolatile(object, offset, (Boolean) value);
                } else {
                    UNSAFE.putBoolean(object, offset, (Boolean) value);
                }
            } else if (field.getType() == Integer.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    UNSAFE.putIntVolatile(object, offset, ((Number) value).intValue());
                } else {
                    UNSAFE.putInt(object, offset, ((Number) value).intValue());
                }
            } else if (field.getType() == Long.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    UNSAFE.putLongVolatile(object, offset, ((Number) value).longValue());
                } else {
                    UNSAFE.putLong(object, offset, ((Number) value).longValue());
                }
            } else if (field.getType() == Short.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    UNSAFE.putShortVolatile(object, offset, ((Number) value).shortValue());
                } else {
                    UNSAFE.putShort(object, offset, ((Number) value).shortValue());
                }
            } else if (field.getType() == Byte.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    UNSAFE.putByteVolatile(object, offset, ((Number) value).byteValue());
                } else {
                    UNSAFE.putByte(object, offset, ((Number) value).byteValue());
                }
            } else if (field.getType() == Double.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    UNSAFE.putDoubleVolatile(object, offset, ((Number) value).doubleValue());
                } else {
                    UNSAFE.putDouble(object, offset, ((Number) value).doubleValue());
                }
            } else if (field.getType() == Float.TYPE && value instanceof Number) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    UNSAFE.putFloatVolatile(object, offset, ((Number) value).floatValue());
                } else {
                    UNSAFE.putFloat(object, offset, ((Number) value).floatValue());
                }
            } else if (field.getType() == Character.TYPE && value instanceof Character) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    UNSAFE.putCharVolatile(object, offset, (Character) value);
                } else {
                    UNSAFE.putChar(object, offset, (Character) value);
                }
            } else {
                if (Modifier.isVolatile(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) { // TODO: use *volatile for other final fields as well
                    // TODO: switch to putReferenceVolatile from jdk.internal.reflect.Unsafe since it provdies better object visibility
                    UNSAFE.putObjectVolatile(object, offset, value);
                } else {
                    UNSAFE.putObject(object, offset, value);
                }
            }
            
        } catch (Throwable e) {
            throw new UnsafeInvocationException(e);
        }
    }

    public T get() throws UnsafeInvocationException {
        try {

            sun.misc.Unsafe UNSAFE = Unsafe.getSunMiscUnsafe();

            /*
            Ensure the given class has been initialized. This is often needed in conjunction with obtaining the static field base of a class.
             */
            UNSAFE.ensureClassInitialized(field.getDeclaringClass());
            
            long offset = UNSAFE.staticFieldOffset(field);

            Object object = field.getDeclaringClass();
            if (field.getType() == Boolean.TYPE) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    return (T) (Boolean) UNSAFE.getBooleanVolatile(object, offset);
                } else {
                    return (T) (Boolean) UNSAFE.getBoolean(object, offset);
                }
            } else if (field.getType() == Integer.TYPE) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    return (T) (Integer) UNSAFE.getIntVolatile(object, offset);
                } else {
                    return (T) (Integer) UNSAFE.getInt(object, offset);
                }
            } else if (field.getType() == Long.TYPE) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    return (T) (Long) UNSAFE.getLongVolatile(object, offset);
                } else {
                    return (T) (Long) UNSAFE.getLong(object, offset);
                }
            } else if (field.getType() == Short.TYPE) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    return (T) (Short) UNSAFE.getShortVolatile(object, offset);
                } else {
                    return (T) (Short) UNSAFE.getShort(object, offset);
                }
            } else if (field.getType() == Byte.TYPE) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    return (T) (Byte) UNSAFE.getByteVolatile(object, offset);
                } else {
                    return (T) (Byte) UNSAFE.getByte(object, offset);
                }
            } else if (field.getType() == Double.TYPE) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    return (T) (Double) UNSAFE.getDoubleVolatile(object, offset);
                } else {
                    return (T) (Double) UNSAFE.getDouble(object, offset);
                }
            } else if (field.getType() == Float.TYPE) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    return (T) (Float) UNSAFE.getFloatVolatile(object, offset);
                } else {
                    return (T) (Float) UNSAFE.getFloat(object, offset);
                }
            } else if (field.getType() == Character.TYPE) {
                if (Modifier.isVolatile(field.getModifiers())) {
                    return (T) (Character) UNSAFE.getCharVolatile(object, offset);
                } else {
                    return (T) (Character) UNSAFE.getChar(object, offset);
                }
            } else {
                if (Modifier.isVolatile(field.getModifiers())) {
                    return (T) UNSAFE.getObjectVolatile(object, offset);
                } else {
                    return (T) UNSAFE.getObject(object, offset);
                }
            }
            
        } catch (Throwable e) {
            throw new UnsafeInvocationException(e);
        } 
    }

}
