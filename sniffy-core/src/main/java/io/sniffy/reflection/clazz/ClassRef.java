package io.sniffy.reflection.clazz;

import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.Unsafe;
import io.sniffy.reflection.UnsafeInvocationException;
import io.sniffy.reflection.constructor.UnresolvedZeroArgsClassConstructorRef;
import io.sniffy.reflection.constructor.ZeroArgsClassConstructorRef;
import io.sniffy.reflection.field.*;
import io.sniffy.reflection.method.*;
import io.sniffy.reflection.module.ModuleRef;
import io.sniffy.reflection.module.UnresolvedModuleRef;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.sniffy.reflection.Unsafe.$;

// TODO: use declaredFields0 and declaredMethods0 to also cover super system methods
public class ClassRef<C> {

    private final Class<C> clazz;

    public ClassRef(Class<C> clazz) {
        this.clazz = clazz;
    }

    public UnresolvedModuleRef getModuleRef() {
        try {
            Class<?> moduleClass = Class.forName("java.lang.Module");
            //noinspection rawtypes
            ClassRef<Class> classClassRef = $(Class.class);
            //noinspection rawtypes
            UnresolvedNonStaticNonVoidMethodRef<Class, ?> getModuleMethodRef = classClassRef.getNonStaticMethod(moduleClass, "getModule");
            Object module = getModuleMethodRef.invoke(clazz);
            return new UnresolvedModuleRef(new ModuleRef(module), null);
        } catch (Throwable e) {
            return new UnresolvedModuleRef(null, e);
        }
    }

    // TODO: should it be C instead of C1 here?
    public <T> UnresolvedNonStaticFieldRef<C, T> findFirstNonStaticField(FieldFilter fieldFilter, boolean recursive) {
        try {
            Class<? super C> clazz = this.clazz;
            while (clazz != Object.class) {
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (!Modifier.isStatic(declaredField.getModifiers()) && (null == fieldFilter || fieldFilter.include(declaredField.getName(), declaredField))) {
                        return new UnresolvedNonStaticFieldRef<C, T>(new NonStaticFieldRef<C, T>(declaredField), null);
                    }
                }
                if (recursive) {
                    clazz = clazz.getSuperclass();
                } else {
                    break;
                }
            }
            return new UnresolvedNonStaticFieldRef<C, T>(null, new NoSuchFieldError());
        } catch (Throwable e) {
            return new UnresolvedNonStaticFieldRef<C, T>(null, e);
        }
    }

    public Map<String, NonStaticFieldRef<C,Object>> findNonStaticFields(FieldFilter fieldFilter, boolean recursive) {
        Map<String, NonStaticFieldRef<C, Object>> fields = new HashMap<String, NonStaticFieldRef<C, Object>>();
        Class<? super C> clazz = this.clazz;
        while (clazz != Object.class) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                if (!Modifier.isStatic(field.getModifiers()) && (null == fieldFilter || fieldFilter.include(field.getName(), field))) {
                    fields.put(field.getName(), new NonStaticFieldRef<C, Object>(field));
                }
            }
            if (recursive) {
                clazz = clazz.getSuperclass();
            } else {
                break;
            }
        }
        return fields;
    }

    public <T> UnresolvedStaticFieldRef<T> findFirstStaticField(FieldFilter fieldFilter, boolean recursive) {
        try {
            Class<? super C> clazz = this.clazz;
            while (clazz != Object.class) {
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (Modifier.isStatic(declaredField.getModifiers()) && (null == fieldFilter || fieldFilter.include(declaredField.getName(), declaredField))) {
                        return new UnresolvedStaticFieldRef<T>(new StaticFieldRef<T>(declaredField), null);
                    }
                }
                if (recursive) {
                    clazz = clazz.getSuperclass();
                } else {
                    break;
                }
            }
            return new UnresolvedStaticFieldRef<T>(null, new NoSuchFieldError());
        } catch (Throwable e) {
            return new UnresolvedStaticFieldRef<T>(null, e);
        }
    }

    public Map<String, StaticFieldRef<Object>> findStaticFields(FieldFilter fieldFilter, boolean recursive) {
        Map<String, StaticFieldRef<Object>> fields = new HashMap<String, StaticFieldRef<Object>>();
        Class<? super C> clazz = this.clazz;
        while (clazz != Object.class) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                if (Modifier.isStatic(field.getModifiers()) && (null == fieldFilter || fieldFilter.include(field.getName(), field))) {
                    fields.put(field.getName(), new StaticFieldRef<Object>(field));
                }
            }
            if (recursive) {
                clazz = clazz.getSuperclass();
            } else {
                break;
            }
        }
        return fields;
    }

    public void copyFields(C from, C to) throws UnsafeInvocationException {

        // TODO: introduce caching here
        for (NonStaticFieldRef<C,Object> fieldRef : findNonStaticFields(null, true).values()) {
            fieldRef.copy(from, to);
        }

    }

    public <T> UnresolvedStaticFieldRef<T> getStaticField(String fieldName) {
        return findFirstStaticField(FieldFilters.byName(fieldName), true);
    }

    public <T> UnresolvedNonStaticFieldRef<C,T> getNonStaticField(String fieldName) {
        return findFirstNonStaticField(FieldFilters.byName(fieldName), true);
    }

    // TODO: add "field" method which would act as a synonym for getNonStaticField or return something generic

    // TODO: add helper methods for getting constructors with arguments

    public UnresolvedZeroArgsClassConstructorRef<C> getConstructor() {
        try {
            Constructor<C> declaredConstructor = clazz.getDeclaredConstructor();
            if (Unsafe.setAccessible(declaredConstructor)) {
                return new UnresolvedZeroArgsClassConstructorRef<C>(
                        new ZeroArgsClassConstructorRef<C>(declaredConstructor),
                        null
                );
            } else {
                return new UnresolvedZeroArgsClassConstructorRef<C>(null, new UnresolvedRefException("Constructor " + clazz.getName() + "." + declaredConstructor.getName() + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new UnresolvedZeroArgsClassConstructorRef<C>(null, e);
        }
    }

    // methods


    public Map<MethodKey, NonStaticMethodRef<C>> getNonStaticMethods(Class<? super C> upperBound, MethodFilter methodFilter) {
        Map<MethodKey, NonStaticMethodRef<C>> methodRefs = new HashMap<MethodKey, NonStaticMethodRef<C>>();
        Class<? super C> clazz = this.clazz;
        while (clazz != (null == upperBound ? Object.class : upperBound)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    MethodKey methodKey = new MethodKey(method);
                    if (null == methodFilter || methodFilter.include(methodKey, method)) {
                        methodRefs.put(methodKey, new NonStaticMethodRef<C>(method));
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        return methodRefs;
    }

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

    // new method factories



    @SuppressWarnings("Convert2Diamond")
    public UnresolvedNonStaticMethodRef<C> getNonStaticMethod(String methodName, Class<?>... parameterTypes) {
        try {
            Method declaredMethod = getDeclaredMethod(methodName, parameterTypes);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new UnresolvedNonStaticMethodRef<C>(new NonStaticMethodRef<C>(declaredMethod), null);
            } else {
                return new UnresolvedNonStaticMethodRef<C>(null, new UnresolvedRefException("Method " + clazz.getName() + "." + methodName + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new UnresolvedNonStaticMethodRef<C>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T> UnresolvedNonStaticNonVoidMethodRef<C,T> getNonStaticMethod(Class<T> returnType, String methodName, Class<?>... parameterTypes) {
        try {
            Method declaredMethod = getDeclaredMethod(methodName, parameterTypes);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new UnresolvedNonStaticNonVoidMethodRef<C,T>(new NonStaticNonVoidMethodRef<C,T>(declaredMethod), null);
            } else {
                return new UnresolvedNonStaticNonVoidMethodRef<C,T>(null, new UnresolvedRefException("Method " + clazz.getName() + "." + methodName + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new UnresolvedNonStaticNonVoidMethodRef<C,T>(null, e);
        }
    }
    @SuppressWarnings("Convert2Diamond")
    public UnresolvedStaticMethodRef getStaticMethod(String methodName, Class<?>... parameterTypes) {
        try {
            Method declaredMethod = getDeclaredMethod(methodName, parameterTypes);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new UnresolvedStaticMethodRef(new StaticMethodRef(declaredMethod), null);
            } else {
                return new UnresolvedStaticMethodRef(null, new UnresolvedRefException("Method " + clazz.getName() + "." + methodName + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new UnresolvedStaticMethodRef(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T> UnresolvedStaticNonVoidMethodRef<T> getStaticMethod(Class<T> returnType, String methodName, Class<?>... parameterTypes) {
        try {
            Method declaredMethod = getDeclaredMethod(methodName, parameterTypes);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new UnresolvedStaticNonVoidMethodRef<T>(new StaticNonVoidMethodRef<T>(declaredMethod), null);
            } else {
                return new UnresolvedStaticNonVoidMethodRef<T>(null, new UnresolvedRefException("Method " + clazz.getName() + "." + methodName + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new UnresolvedStaticNonVoidMethodRef<T>(null, e);
        }
    }

}
