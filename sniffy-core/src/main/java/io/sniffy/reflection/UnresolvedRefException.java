package io.sniffy.reflection;

import javax.annotation.Nonnull;

/**
 * This checked exception is thrown when trying to resolve an {@link UnresolvedRef} which was initiated with an exception.
 * @see UnresolvedRef
 */
public class UnresolvedRefException extends Exception {

    public UnresolvedRefException() {
    }

    public UnresolvedRefException(@Nonnull String message) {
        super(message);
    }

    public UnresolvedRefException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

    public UnresolvedRefException(@Nonnull Throwable cause) {
        super(cause);
    }

}
