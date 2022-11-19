package io.sniffy.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClassRef<C> implements ResolvableRef {

    private final Class<C> clazz;

    public ClassRef(Class<C> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean isResolved() {
        return null != clazz;
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, C1> FieldRef<? super C1, T> firstField(String fieldName) {
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
            return new FieldRef<C1, T>(null, firstException);
        } catch (Throwable e) {
            return new FieldRef<C1, T>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T> FieldRef<C, T> field(String fieldName) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            return new FieldRef<C, T>(declaredField, null);
        } catch (Throwable e) {
            return new FieldRef<C, T>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public VoidZeroArgsMethodRef<C> method(String methodName) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new VoidZeroArgsMethodRef<C>(declaredMethod, null);
            } else {
                return new VoidZeroArgsMethodRef<C>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new VoidZeroArgsMethodRef<C>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <P1> VoidOneArgMethodRef<C, P1> method(String methodName, Class<P1> p1Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new VoidOneArgMethodRef<C, P1>(declaredMethod, null);
            } else {
                return new VoidOneArgMethodRef<C, P1>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new VoidOneArgMethodRef<C, P1>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <P1, P2> VoidTwoArgsMethodRef<C, P1, P2> method(String methodName, Class<P1> p1Class, Class<P2> p2Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new VoidTwoArgsMethodRef<C, P1, P2>(declaredMethod, null);
            } else {
                return new VoidTwoArgsMethodRef<C, P1, P2>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new VoidTwoArgsMethodRef<C, P1, P2>(null, e);
        }
    }

}
