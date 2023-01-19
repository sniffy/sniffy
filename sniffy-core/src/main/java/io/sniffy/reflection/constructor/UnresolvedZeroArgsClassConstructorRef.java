package io.sniffy.reflection.constructor;

import io.sniffy.reflection.UnresolvedRef;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.UnsafeInvocationException;

public class UnresolvedZeroArgsClassConstructorRef<C> extends UnresolvedRef<ZeroArgsClassConstructorRef<C>> {

    public UnresolvedZeroArgsClassConstructorRef(ZeroArgsClassConstructorRef<C> ref, Throwable throwable) {
        super(ref, throwable);
    }

    public C newInstanceOrNull() {
        try {
            return newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    public C newInstance() throws UnsafeInvocationException, UnresolvedRefException {
        return resolve().newInstance();
    }

}
