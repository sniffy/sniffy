package com.github.bedrin.jdbc.sniffer;

public class WrongNumberOfQueriesError extends AssertionError {

    public WrongNumberOfQueriesError(Object detailMessage) {
        super(detailMessage);
    }

    public WrongNumberOfQueriesError() {
    }

    public WrongNumberOfQueriesError(String message, Throwable cause) {
        super(message, cause);
    }

}
