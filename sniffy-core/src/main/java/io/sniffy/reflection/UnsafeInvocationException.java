package io.sniffy.reflection;

public class UnsafeInvocationException extends Exception {

    public UnsafeInvocationException() {
    }

    public UnsafeInvocationException(String message) {
        super(message);
    }

    public UnsafeInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsafeInvocationException(Throwable cause) {
        super(cause);
    }

}
