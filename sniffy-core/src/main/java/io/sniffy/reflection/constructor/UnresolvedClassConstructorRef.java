package io.sniffy.reflection.constructor;

import io.sniffy.reflection.UnresolvedRef;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.UnsafeInvocationException;

public class UnresolvedClassConstructorRef<C> extends UnresolvedRef<ClassConstructorRef<C>> {

    public UnresolvedClassConstructorRef(ClassConstructorRef<C> ref, Throwable throwable) {
        super(ref, throwable);
    }

    public C newInstanceOrNull(Object... parameters) {
        try {
            return newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    public C newInstance(Object... parameters) throws UnsafeInvocationException, UnresolvedRefException {
        return resolve().newInstance();
    }

}
