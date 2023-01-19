package io.sniffy.reflection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wraps a pair of non-null object of class T and exception which could have been thrown when obtaining this object.
 * Nullable method `T getRef()` is missing intentionally since the idea is to enforcer developer to catch the potential checked exception of type {@link UnresolvedRefException}.
 *
 * @param <T>
 */
public class UnresolvedRef<T> {

    @Nullable
    protected final Throwable throwable;
    @Nullable
    protected final T ref;

    public UnresolvedRef(@Nullable T ref, @Nullable Throwable throwable) {
        assert null == ref || null == throwable;
        this.throwable = throwable;
        this.ref = ref;
    }

    public @Nonnull T resolve() throws UnresolvedRefException {
        if (isResolved()) {
            //noinspection ConstantConditions
            return ref;
        } else {
            throw new UnresolvedRefException(throwable);
        }
    }

    public boolean isResolved() {
        return null == throwable;
    }

    public @Nullable Throwable getResolveException() {
        return throwable;
    }

}
