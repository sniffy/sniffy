package io.sniffy.reflection;

import javax.annotation.Nonnull;

/**
 * General checked exception throw by Sniffy Unsafe framework in case of general issues
 */
public class UnsafeException extends Exception {

    public UnsafeException() {
    }

    public UnsafeException(@Nonnull String message) {
        super(message);
    }

    public UnsafeException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

    public UnsafeException(@Nonnull Throwable cause) {
        super(cause);
    }

}
