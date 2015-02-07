package com.github.bedrin.jdbc.sniffer;

public class RecordedQueriesWithValue<T> extends RecordedQueries<RecordedQueriesWithValue<T>> {

    private final T value;

    RecordedQueriesWithValue(T value, int executedStatements, int executedThreadLocalStatements, int executedOtherThreadsStatements) {
        super(executedStatements, executedThreadLocalStatements, executedOtherThreadsStatements);
        this.value = value;
    }

    public T getValue() {
        return value;
    }

}
