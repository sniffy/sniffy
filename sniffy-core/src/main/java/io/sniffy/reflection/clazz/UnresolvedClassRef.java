package io.sniffy.reflection.clazz;

import io.sniffy.reflection.UnresolvedRef;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.constructor.UnresolvedZeroArgsClassConstructorRef;
import io.sniffy.reflection.field.*;
import io.sniffy.reflection.method.UnresolvedNonStaticMethodRef;
import io.sniffy.reflection.method.UnresolvedNonStaticNonVoidMethodRef;
import io.sniffy.reflection.method.UnresolvedStaticMethodRef;
import io.sniffy.reflection.method.UnresolvedStaticNonVoidMethodRef;
import io.sniffy.reflection.module.UnresolvedModuleRef;

import java.util.Map;

public class UnresolvedClassRef<C> extends UnresolvedRef<ClassRef<C>> {

    public UnresolvedClassRef(ClassRef<C> ref, Throwable throwable) {
        super(ref, throwable);
    }

    public UnresolvedModuleRef tryGetModuleRef() {
        try {
            return getModuleRef();
        } catch (UnresolvedRefException e) {
            return new UnresolvedModuleRef(null, e);
        }
    }

    public UnresolvedModuleRef getModuleRef() throws UnresolvedRefException {
        return resolve().getModuleRef();
    }

    public UnresolvedZeroArgsClassConstructorRef<C> tryGetConstructor() {
        try {
            return getConstructor();
        } catch (UnresolvedRefException e) {
            return new UnresolvedZeroArgsClassConstructorRef<C>(null, e);
        }
    }

    public UnresolvedZeroArgsClassConstructorRef<C> getConstructor() throws UnresolvedRefException {
        return resolve().getConstructor();
    }

    public <T> UnresolvedStaticFieldRef<T> tryGetStaticField(String fieldName) {
        try {
            return getStaticField(fieldName);
        } catch (Throwable e) {
            return new UnresolvedStaticFieldRef<T>(null, e);
        }
    }

    public <T> UnresolvedStaticFieldRef<T> getStaticField(String fieldName) throws UnresolvedRefException {
        return resolve().getStaticField(fieldName);
    }

    public <T> UnresolvedNonStaticFieldRef<C, T> tryGetNonStaticField(String fieldName) {
        try {
            return getNonStaticField(fieldName);
        } catch (Throwable e) {
            return new UnresolvedNonStaticFieldRef<C, T>(null, e);
        }
    }

    public <T> UnresolvedNonStaticFieldRef<C, T> getNonStaticField(String fieldName) throws UnresolvedRefException {
        return resolve().getNonStaticField(fieldName);
    }

    public <T> UnresolvedNonStaticFieldRef<C, T> findFirstNonStaticField(FieldFilter fieldFilter, boolean recursive) throws UnresolvedRefException {
        return resolve().findFirstNonStaticField(fieldFilter, recursive);
    }

    public Map<String, NonStaticFieldRef<C, Object>> findNonStaticFields(FieldFilter fieldFilter, boolean recursive) throws UnresolvedRefException {
        return resolve().findNonStaticFields(fieldFilter, recursive);
    }

    public <T> UnresolvedStaticFieldRef<T> tryFindFirstStaticField(FieldFilter fieldFilter, boolean recursive) {
        try {
            return findFirstStaticField(fieldFilter, recursive);
        } catch (Throwable e) {
            return new UnresolvedStaticFieldRef<T>(null, e);
        }
    }

    public <T> UnresolvedStaticFieldRef<T> findFirstStaticField(FieldFilter fieldFilter, boolean recursive) throws UnresolvedRefException {
        return resolve().findFirstStaticField(fieldFilter, recursive);
    }

    public Map<String, StaticFieldRef<Object>> findStaticFields(FieldFilter fieldFilter, boolean recursive) throws UnresolvedRefException {
        return resolve().findStaticFields(fieldFilter, recursive);
    }

    // methods

    public UnresolvedNonStaticMethodRef<C> getNonStaticMethod(String methodName, Class<?>... parameterTypes) throws UnresolvedRefException {
        return resolve().getNonStaticMethod(methodName, parameterTypes);
    }

    public <T> UnresolvedNonStaticNonVoidMethodRef<C,T> getNonStaticMethod(Class<T> returnType, String methodName, Class<?>... parameterTypes) throws UnresolvedRefException {
        return resolve().getNonStaticMethod(returnType, methodName, parameterTypes);
    }

    public UnresolvedStaticMethodRef getStaticMethod(String methodName, Class<?>... parameterTypes) throws UnresolvedRefException {
        return resolve().getStaticMethod(methodName, parameterTypes);
    }

    public <T> UnresolvedStaticNonVoidMethodRef<T> getStaticMethod(Class<T> returnType, String methodName, Class<?>... parameterTypes) throws UnresolvedRefException {
        return resolve().getStaticMethod(returnType, methodName, parameterTypes);
    }

}
