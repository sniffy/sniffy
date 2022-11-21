package io.sniffy.reflection;

import io.sniffy.reflection.constructor.ConstructorRefBuilder;
import io.sniffy.reflection.constructor.ZeroArgsConstructorRef;
import io.sniffy.reflection.field.FieldRef;
import io.sniffy.reflection.method.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static io.sniffy.reflection.Unsafe.$;

public class ClassRef<C> implements ResolvableRef {

    private final Class<C> clazz;

    public ClassRef(Class<C> clazz) {
        this.clazz = clazz;
    }

    public <T> ClassRef<T> superClassRef(Class<T> clazz) throws UnsafeException {
        if (clazz.isAssignableFrom(this.clazz)) {
            return $(clazz);
        } else {
            throw new UnsafeException("Cannot cast " + this.clazz + " to " + clazz);
        }
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

    // esoteric

    @SuppressWarnings("RedundantSuppression")
    public ZeroArgsConstructorRef<C> constructor() throws UnsafeException {
        return ConstructorRefBuilder.constructor(clazz);
    }

    // void method factories

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

    @SuppressWarnings("Convert2Diamond")
    public <P1, P2, P3> VoidThreeArgsMethodRef<C, P1, P2, P3> method(String methodName, Class<P1> p1Class, Class<P2> p2Class, Class<P3> p3Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class, p3Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new VoidThreeArgsMethodRef<C, P1, P2, P3>(declaredMethod, null);
            } else {
                return new VoidThreeArgsMethodRef<C, P1, P2, P3>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new VoidThreeArgsMethodRef<C, P1, P2, P3>(null, e);
        }
    }

    // TODO: handle case of more arguments

    // non-void method factories


    @SuppressWarnings("Convert2Diamond")
    public <T> NonVoidZeroArgsMethodRef<T, C> method(@SuppressWarnings("unused") Class<T> tClass, String methodName) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidZeroArgsMethodRef<T, C>(declaredMethod, null);
            } else {
                return new NonVoidZeroArgsMethodRef<T, C>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidZeroArgsMethodRef<T, C>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, P1> NonVoidOneArgMethodRef<T, C, P1> method(@SuppressWarnings("unused") Class<T> tClass, String methodName, Class<P1> p1Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidOneArgMethodRef<T, C, P1>(declaredMethod, null);
            } else {
                return new NonVoidOneArgMethodRef<T, C, P1>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidOneArgMethodRef<T, C, P1>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, P1, P2> NonVoidTwoArgsMethodRef<T, C, P1, P2> method(@SuppressWarnings("unused") Class<T> tClass, String methodName, Class<P1> p1Class, Class<P2> p2Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidTwoArgsMethodRef<T, C, P1, P2>(declaredMethod, null);
            } else {
                return new NonVoidTwoArgsMethodRef<T, C, P1, P2>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidTwoArgsMethodRef<T, C, P1, P2>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, P1, P2, P3> NonVoidThreeArgsMethodRef<T, C, P1, P2, P3> method(@SuppressWarnings("unused") Class<T> tClass, String methodName, Class<P1> p1Class, Class<P2> p2Class, Class<P3> p3Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class, p3Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidThreeArgsMethodRef<T, C, P1, P2, P3>(declaredMethod, null);
            } else {
                return new NonVoidThreeArgsMethodRef<T, C, P1, P2, P3>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidThreeArgsMethodRef<T, C, P1, P2, P3>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, P1, P2, P3, P4> NonVoidFourArgsMethodRef<T, C, P1, P2, P3, P4> method(@SuppressWarnings("unused") Class<T> tClass, String methodName, Class<P1> p1Class, Class<P2> p2Class, Class<P3> p3Class, Class<P4> p4Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class, p3Class, p4Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidFourArgsMethodRef<T, C, P1, P2, P3, P4>(declaredMethod, null);
            } else {
                return new NonVoidFourArgsMethodRef<T, C, P1, P2, P3, P4>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidFourArgsMethodRef<T, C, P1, P2, P3, P4>(null, e);
        }
    }

    // TODO: handle case of more arguments

}
