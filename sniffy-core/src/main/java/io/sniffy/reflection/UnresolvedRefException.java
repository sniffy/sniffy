package io.sniffy.reflection;

public class UnresolvedRefException extends Exception {

    public UnresolvedRefException() {
    }

    public UnresolvedRefException(String message) {
        super(message);
    }

    public UnresolvedRefException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnresolvedRefException(Throwable cause) {
        super(cause);
    }

}
