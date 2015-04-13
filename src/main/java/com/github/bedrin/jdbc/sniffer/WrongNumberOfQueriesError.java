package com.github.bedrin.jdbc.sniffer;

public class WrongNumberOfQueriesError extends AssertionError {

    public WrongNumberOfQueriesError(Object detailMessage) {
        super(detailMessage);
    }

}
