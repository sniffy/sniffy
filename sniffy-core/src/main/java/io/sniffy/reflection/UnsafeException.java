package io.sniffy.reflection;

public class UnsafeException extends Exception {

    public UnsafeException() {
    }

    public UnsafeException(String message) {
        super(message);
    }

    public UnsafeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsafeException(Throwable cause) {
        super(cause);
    }

    public UnsafeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
