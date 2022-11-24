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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Unresolved reference to class
 * @see ClassRef
 * @see UnresolvedRef
 * @param <C>
 */
public class UnresolvedClassRef<C> extends UnresolvedRef<ClassRef<C>> {

    public UnresolvedClassRef(@Nullable ClassRef<C> ref, @Nullable Throwable throwable) {
        super(ref, throwable);
    }

    public @Nonnull UnresolvedModuleRef tryGetModuleRef() {
        try {
            return getModuleRef();
        } catch (UnresolvedRefException e) {
            return new UnresolvedModuleRef(null, e);
        }
    }

    public @Nonnull UnresolvedModuleRef getModuleRef() throws UnresolvedRefException {
        return resolve().getModuleRef();
    }

    public @Nonnull UnresolvedZeroArgsClassConstructorRef<C> tryGetConstructor() {
        try {
            return getConstructor();
        } catch (UnresolvedRefException e) {
            return new UnresolvedZeroArgsClassConstructorRef<C>(null, e);
        }
    }

    public @Nonnull UnresolvedZeroArgsClassConstructorRef<C> getConstructor() throws UnresolvedRefException {
        return resolve().getConstructor();
    }

    public <T> @Nonnull UnresolvedStaticFieldRef<T> tryGetStaticField(@Nonnull String fieldName) {
        try {
            return getStaticField(fieldName);
        } catch (Throwable e) {
            return new UnresolvedStaticFieldRef<T>(null, e);
        }
    }

    public <T> @Nonnull UnresolvedStaticFieldRef<T> getStaticField(@Nonnull String fieldName) throws UnresolvedRefException {
        return resolve().getStaticField(fieldName);
    }

    public <T> @Nonnull UnresolvedNonStaticFieldRef<C, T> tryGetNonStaticField(@Nonnull String fieldName) {
        try {
            return getNonStaticField(fieldName);
        } catch (Throwable e) {
            return new UnresolvedNonStaticFieldRef<C, T>(null, e);
        }
    }

    public <T> @Nonnull UnresolvedNonStaticFieldRef<C, T> getNonStaticField(@Nonnull String fieldName) throws UnresolvedRefException {
        return resolve().getNonStaticField(fieldName);
    }

    public <T> @Nonnull UnresolvedNonStaticFieldRef<C, T> findFirstNonStaticField(@Nullable FieldFilter fieldFilter, boolean recursive) throws UnresolvedRefException {
        return resolve().findFirstNonStaticField(fieldFilter, recursive);
    }

    public @Nonnull Map<String, NonStaticFieldRef<C, Object>> findNonStaticFields(@Nullable FieldFilter fieldFilter, boolean recursive) throws UnresolvedRefException {
        return resolve().findNonStaticFields(fieldFilter, recursive);
    }

    public <T> @Nonnull UnresolvedStaticFieldRef<T> tryFindFirstStaticField(@Nullable FieldFilter fieldFilter, boolean recursive) {
        try {
            return findFirstStaticField(fieldFilter, recursive);
        } catch (Throwable e) {
            return new UnresolvedStaticFieldRef<T>(null, e);
        }
    }

    public <T> @Nonnull UnresolvedStaticFieldRef<T> findFirstStaticField(@Nullable FieldFilter fieldFilter, boolean recursive) throws UnresolvedRefException {
        return resolve().findFirstStaticField(fieldFilter, recursive);
    }

    public @Nonnull Map<String, StaticFieldRef<Object>> findStaticFields(@Nonnull FieldFilter fieldFilter, boolean recursive) throws UnresolvedRefException {
        return resolve().findStaticFields(fieldFilter, recursive);
    }

    // methods

    public @Nonnull UnresolvedNonStaticMethodRef<C> getNonStaticMethod(@Nonnull String methodName, @Nonnull Class<?>... parameterTypes) throws UnresolvedRefException {
        return resolve().getNonStaticMethod(methodName, parameterTypes);
    }

    public <T> @Nonnull UnresolvedNonStaticNonVoidMethodRef<C,T> getNonStaticMethod(@Nullable Class<T> returnType, @Nonnull String methodName, @Nonnull Class<?>... parameterTypes) throws UnresolvedRefException {
        return resolve().getNonStaticMethod(returnType, methodName, parameterTypes);
    }

    public @Nonnull UnresolvedStaticMethodRef getStaticMethod(@Nonnull String methodName, @Nonnull Class<?>... parameterTypes) throws UnresolvedRefException {
        return resolve().getStaticMethod(methodName, parameterTypes);
    }

    public <T> @Nonnull UnresolvedStaticNonVoidMethodRef<T> getStaticMethod(@Nullable Class<T> returnType, @Nonnull String methodName, @Nonnull Class<?>... parameterTypes) throws UnresolvedRefException {
        return resolve().getStaticMethod(returnType, methodName, parameterTypes);
    }

}
