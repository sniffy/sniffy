package io.sniffy.reflection;

public abstract class UnresolvedRef<T> {

    protected final Throwable throwable;
    protected final T ref;

    public UnresolvedRef(T ref, Throwable throwable) {
        this.throwable = throwable;
        this.ref = ref;
    }

    public T resolve() throws UnresolvedRefException {
        if (isResolved()) {
            return ref;
        } else {
            throw new UnresolvedRefException(throwable);
        }
    }

    public boolean isResolved() {
        return null == throwable;
    }

    public Throwable getResolveException() {
        return throwable;
    }

}
