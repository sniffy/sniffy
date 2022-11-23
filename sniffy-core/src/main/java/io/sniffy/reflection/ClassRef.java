package io.sniffy.reflection;

import io.sniffy.reflection.constructor.ZeroArgsConstructorRef;
import io.sniffy.reflection.field.FieldFilter;
import io.sniffy.reflection.field.FieldRef;
import io.sniffy.reflection.method.*;
import io.sniffy.reflection.module.ModuleRef;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.sniffy.reflection.Unsafe.$;

// TODO: use declaredFields0 and declaredMethods0 to also cover super system methods
public class ClassRef<C> implements ResolvableRef {

    private final Class<C> clazz;
    private final Throwable throwable;

    public Class<C> getEnclosedClass() {
        return clazz;
    }

    public Throwable getException() {
        return throwable;
    }

    public ClassRef(Class<C> clazz, Throwable throwable) {
        this.clazz = clazz;
        this.throwable = throwable;
    }

    public <T> ClassRef<T> superClassRef(Class<T> clazz) throws UnsafeException {
        if (clazz.isAssignableFrom(this.clazz)) {
            return $(clazz);
        } else {
            throw new UnsafeException("Cannot cast " + this.clazz + " to " + clazz);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ClassRef<T> cast(@SuppressWarnings("unused") Class<T> unused) throws UnsafeException {
        return (ClassRef<T>) this;
    }

    @Override
    public boolean isResolved() {
        return null != clazz;
    }

    public ModuleRef moduleRef() {
        try {
            Class<?> moduleClass = Class.forName("java.lang.Module");
            //noinspection rawtypes
            ClassRef<Class> classClassRef = $(Class.class);
            //noinspection rawtypes
            NonVoidZeroArgsMethodRef<?, Class> getModuleMethodRef = classClassRef.method(moduleClass, "getModule");
            Object module = getModuleMethodRef.invoke(clazz);
            return new ModuleRef(module, null);
        } catch (Throwable e) {
            return new ModuleRef(null, e);
        }
    }

    // TODO: should it be C instead of C1 here?
    public <T> FieldRef<? super C, T> findFirstField(FieldFilter fieldFilter, boolean recursive) {
        try {
            Class<? super C> clazz = this.clazz;
            while (clazz != Object.class) {
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (fieldFilter.include(declaredField.getName(), declaredField)) {
                        return new FieldRef<C, T>(declaredField, null);
                    }
                }
                if (recursive) {
                    clazz = clazz.getSuperclass();
                } else {
                    break;
                }
            }
            return new FieldRef<C, T>(null, new NoSuchFieldError());
        } catch (Throwable e) {
            return new FieldRef<C, T>(null, e);
        }
    }

    // TODO: return fields from java.lang.Object as well
    public Map<String, FieldRef<? super C,Object>> findFields(FieldFilter fieldFilter, boolean recursive) {
        Map<String, FieldRef<? super C, Object>> fields = new HashMap<String, FieldRef<? super C, Object>>();
        try {
            Class<? super C> clazz = this.clazz;
            while (clazz != Object.class) {
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field field : declaredFields) {
                    if (fieldFilter.include(field.getName(), field)) {
                        fields.put(field.getName(), new FieldRef<C, Object>(field, null));
                    }
                }
                if (recursive) {
                    clazz = clazz.getSuperclass();
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO: do something different; change API around resolving references, like getOrDefault...
        }
        return fields;
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
    public <T, C1> FieldRef<? super C1, T> firstField(Class<T> fieldType) {
        try {
            Exception firstException = null;
            Class<? super C> clazz = this.clazz;
            while (clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.getType().equals(fieldType)) {
                        return new FieldRef<C1, T>(field, null);
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return new FieldRef<C1, T>(null, new IllegalStateException());
        } catch (Throwable e) {
            return new FieldRef<C1, T>(null, e);
        }
    }

    public Map<MethodKey, AbstractMethodRef<C>> getMethods(Class<? super C> upperBound, MethodFilter methodFilter) {
        Map<MethodKey, AbstractMethodRef<C>> methodRefs = new HashMap<MethodKey, AbstractMethodRef<C>>();
        Class<? super C> clazz = this.clazz;
        while (clazz != (null == upperBound ? Object.class : upperBound)) {
            for (Method method : clazz.getDeclaredMethods()) {
                MethodKey methodKey = new MethodKey(method);
                if (null == methodFilter || methodFilter.include(methodKey, method)) {
                    methodRefs.put(methodKey, new AbstractMethodRef<C>(method, null));
                }
            }
            clazz = clazz.getSuperclass();
        }

        return methodRefs;
    }

    public Map<String, FieldRef<C, ?>> getDeclaredFields() {
        return getDeclaredFields(true, true);
    }

    // TODO: return fields from java.lang.Object as well
    public Map<String, FieldRef<C, ?>> getDeclaredFields(boolean includeSynthetic, boolean includeStatic) {
        Map<String, FieldRef<C, ?>> fields = new HashMap<String, FieldRef<C, ?>>();
        Class<? super C> clazz = this.clazz;
        while (clazz != Object.class) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                if (
                        (includeSynthetic || !field.isSynthetic()) &&
                                (includeStatic || !Modifier.isStatic(field.getModifiers()))
                ) {
                    fields.put(field.getName(), new FieldRef<C, Object>(field, null));
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    public void copyFields(C from, C to) {

        // TODO: introduce caching here

        try {
            for (FieldRef<C, ?> fieldRef : getDeclaredFields(false, false).values()) {
                fieldRef.copy(from, to);
            }
        } catch (Exception e) {
            throw ExceptionUtil.throwException(e); // TODO: unify exception handling
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

    public ZeroArgsConstructorRef<C> constructor() {
        try {
            Constructor<C> declaredConstructor = clazz.getDeclaredConstructor();
            if (Unsafe.setAccessible(declaredConstructor)) {
                return new ZeroArgsConstructorRef<C>(declaredConstructor, null, null);
            } else {
                return new ZeroArgsConstructorRef<C>(null, null, new UnsafeException("Constructor " + clazz.getName() + "." + declaredConstructor.getName() + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new ZeroArgsConstructorRef<C>(null, null, e);
        }
        //return ConstructorRefBuilder.constructor(clazz);
    }

    // methods

    private Method getDeclaredMethod(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Class<?> clazz = this.clazz;
        while (null != clazz) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                    return method;
                }
            }
            if (clazz == clazz.getSuperclass()) {
                clazz = null;
            } else {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchMethodException();
    }

    // void method factories

    @SuppressWarnings("Convert2Diamond")
    public VoidZeroArgsMethodRef<C> method(String methodName) {
        try {
            Method declaredMethod = getDeclaredMethod(methodName);
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
            Method declaredMethod = getDeclaredMethod(methodName, p1Class);
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
            Method declaredMethod = getDeclaredMethod(methodName, p1Class, p2Class);
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
            Method declaredMethod = getDeclaredMethod(methodName, p1Class, p2Class, p3Class);
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

    public AbstractMethodRef<C> method(String methodName, Class<?>... parameterTypes) {
        try {
            Method declaredMethod = getDeclaredMethod(methodName, parameterTypes);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new AbstractMethodRef<C>(declaredMethod, null);
            } else {
                return new AbstractMethodRef<C>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new AbstractMethodRef<C>(null, e);
        }

    }

    @SuppressWarnings("Convert2Diamond")
    public <T> NonVoidZeroArgsMethodRef<T, C> method(@SuppressWarnings("unused") Class<T> tClass, String methodName) {
        try {
            Method declaredMethod = getDeclaredMethod(methodName);
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
            Method declaredMethod = getDeclaredMethod(methodName, p1Class);
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
            Method declaredMethod = getDeclaredMethod(methodName, p1Class, p2Class);
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
            Method declaredMethod = getDeclaredMethod(methodName, p1Class, p2Class, p3Class);
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
            Method declaredMethod = getDeclaredMethod(methodName, p1Class, p2Class, p3Class, p4Class);
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
