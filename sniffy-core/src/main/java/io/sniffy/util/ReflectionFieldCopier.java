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
                Object o = ReflectionUtil.get(field, source);
                ReflectionUtil.set(field, target, o);
            } catch (IllegalAccessException e) {
                e.printStackTrace(); // TODO: does it fail on Java 19/20 ?
            }
        }
    }

    private static <T> Field getFieldImpl(Class<T> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            ReflectionUtil.setAccessible(field);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
