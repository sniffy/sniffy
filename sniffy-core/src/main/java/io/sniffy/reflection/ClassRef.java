package io.sniffy.reflection;

import java.lang.reflect.Field;

public class ClassRef<C> {

    private final Class<C> clazz;

    public ClassRef(Class<C> clazz) {
        this.clazz = clazz;
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, C1> FieldRef<? super C1,T> firstField(String fieldName) {
        try {
            Exception firstException = null;
            Class<? super C> clazz = this.clazz;
            while (clazz != Object.class) {
                try {
                    Field declaredField = clazz.getDeclaredField(fieldName);
                    return new FieldRef<C1, T>(declaredField, null);
                } catch (NoSuchFieldException e) {
                    if (null == firstException) {
                        firstException = e;
                    }
                    clazz = clazz.getSuperclass();
                }
            }
            return new FieldRef<C1,T>(null, firstException);
        } catch (Throwable e) {
            return new FieldRef<C1,T>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T> FieldRef<C,T> field(String fieldName) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            return new FieldRef<C, T>(declaredField, null);
        } catch (Throwable e) {
            return new FieldRef<C,T>(null, e);
        }
    }

}
