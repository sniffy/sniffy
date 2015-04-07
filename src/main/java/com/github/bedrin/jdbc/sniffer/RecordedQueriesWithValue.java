package com.github.bedrin.jdbc.sniffer;

public class RecordedQueriesWithValue<V> extends ExpectedQueries<RecordedQueriesWithValue<V>> {

    private final V value;

    RecordedQueriesWithValue(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }

}
