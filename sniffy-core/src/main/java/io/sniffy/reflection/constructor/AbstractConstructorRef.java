package io.sniffy.reflection.constructor;

import io.sniffy.reflection.ResolvableRef;

import java.lang.invoke.MethodHandle;

public abstract class AbstractConstructorRef<C> implements ResolvableRef {

    protected final MethodHandle methodHandle;
    protected final Throwable throwable;

    protected AbstractConstructorRef(MethodHandle methodHandle, Throwable throwable) {
        this.methodHandle = methodHandle;
        this.throwable = throwable;
    }

    public boolean isResolved() {
        return null != methodHandle;
    }

    public MethodHandle getMethodHandle() {
        return methodHandle;
    }

    public Throwable getThrowable() {
        return throwable;
    }

}
