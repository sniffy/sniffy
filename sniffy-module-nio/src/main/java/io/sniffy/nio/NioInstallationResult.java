package io.sniffy.nio;

/**
 * Result of installing or removing Sniffy's global NIO provider wrapper.
 */
public final class NioInstallationResult {

    public enum Status {
        INSTALLED,
        ALREADY_INSTALLED,
        UNSUPPORTED,
        FAILED,
        UNINSTALLED
    }

    private final Status status;
    private final String message;
    private final Throwable cause;

    private NioInstallationResult(Status status, String message, Throwable cause) {
        this.status = status;
        this.message = message;
        this.cause = cause;
    }

    static NioInstallationResult of(Status status, String message) {
        return new NioInstallationResult(status, message, null);
    }

    static NioInstallationResult of(Status status, String message, Throwable cause) {
        return new NioInstallationResult(status, message, cause);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    public boolean isInstalled() {
        return status == Status.INSTALLED || status == Status.ALREADY_INSTALLED;
    }

    @Override
    public String toString() {
        return "NioInstallationResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                '}';
    }
}
