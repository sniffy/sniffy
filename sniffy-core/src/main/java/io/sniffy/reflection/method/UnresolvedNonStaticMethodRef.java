package io.sniffy.reflection.method;

import io.sniffy.reflection.UnresolvedRef;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.UnsafeInvocationException;

import java.lang.reflect.InvocationTargetException;

public class UnresolvedNonStaticMethodRef<C> extends UnresolvedRef<NonStaticMethodRef<C>> {

    public UnresolvedNonStaticMethodRef(NonStaticMethodRef<C> ref, Throwable throwable) {
        super(ref, throwable);
    }

    public <T> T invoke(C instance, Object... parameters) throws UnsafeInvocationException, UnresolvedRefException, InvocationTargetException {
        return resolve().invoke(instance, parameters);
    }

}
