package io.sniffy.util;

import java.lang.reflect.Field;

public class ReflectionFieldCopier {

    private final Field field;

    public <T> ReflectionFieldCopier(Class<T> clazz, String fieldName) {
        field = getFieldImpl(clazz, fieldName);
    }

    public boolean isAvailable() {
        return null != field;
    }

    public void copy(Object source, Object target) {
        if (null != field) {
            try {
                Object o = field.get(source);
                field.set(target, o);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static <T> Field getFieldImpl(Class<T> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            return null;
        }
    }

}
