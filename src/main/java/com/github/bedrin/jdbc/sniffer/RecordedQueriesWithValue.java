package com.github.bedrin.jdbc.sniffer;

public class RecordedQueriesWithValue<T> extends ExpectedQueries<RecordedQueriesWithValue<T>> {

    private final T value;

    RecordedQueriesWithValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

}
