package io.sniffy.reflection;

import javax.annotation.Nonnull;

/**
 * Thrown in case of reflection issues with given field, method, constructor or other object
 */
public class UnsafeInvocationException extends Exception {

    public UnsafeInvocationException() {
    }

    public UnsafeInvocationException(@Nonnull String message) {
        super(message);
    }

    public UnsafeInvocationException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

    public UnsafeInvocationException(@Nonnull Throwable cause) {
        super(cause);
    }

}
