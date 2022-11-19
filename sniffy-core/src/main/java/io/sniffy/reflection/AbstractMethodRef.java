package io.sniffy.reflection;

import java.lang.reflect.Method;

public abstract class AbstractMethodRef<C> implements ResolvableRef {

    protected final Method method;
    protected final Throwable throwable;

    protected AbstractMethodRef(Method method, Throwable throwable) {
        this.method = method;
        this.throwable = throwable;
    }

    public boolean isResolved() {
        return null != method;
    }

    public Method getMethod() {
        return method;
    }

    public Throwable getThrowable() {
        return throwable;
    }

}
