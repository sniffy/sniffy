package com.github.bedrin.jdbc.sniffer;

public class RecordedQueriesWithValue<T> extends RecordedQueries {

    private final T value;

    public RecordedQueriesWithValue(T value, int executedStatements, int executedThreadLocalStatements, int executedOtherThreadsStatements) {
        super(executedStatements, executedThreadLocalStatements, executedOtherThreadsStatements);
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public RecordedQueriesWithValue verifyNotMore() {
        super.verifyNotMore();
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotMoreThanOne() {
        super.verifyNotMoreThanOne();
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotMoreThan(int allowedStatements) throws IllegalStateException {
        super.verifyNotMoreThan(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyExact(int allowedStatements) throws IllegalStateException {
        super.verifyExact(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotLessThan(int allowedStatements) throws IllegalStateException {
        super.verifyNotLessThan(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyRange(int minAllowedStatements, int maxAllowedStatements) throws IllegalStateException {
        super.verifyRange(minAllowedStatements, maxAllowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotMoreThreadLocal() {
        super.verifyNotMoreThreadLocal();
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotMoreThanOneThreadLocal() {
        super.verifyNotMoreThanOneThreadLocal();
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotMoreThanThreadLocal(int allowedStatements) throws IllegalStateException {
        super.verifyNotMoreThanThreadLocal(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyExactThreadLocal(int allowedStatements) throws IllegalStateException {
        super.verifyExactThreadLocal(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotLessThanThreadLocal(int allowedStatements) throws IllegalStateException {
        super.verifyNotLessThanThreadLocal(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyRangeThreadLocal(int minAllowedStatements, int maxAllowedStatements) throws IllegalStateException {
        super.verifyRangeThreadLocal(minAllowedStatements, maxAllowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotMoreOtherThreads() {
        super.verifyNotMoreOtherThreads();
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotMoreThanOneOtherThreads() {
        super.verifyNotMoreThanOneOtherThreads();
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotMoreThanOtherThreads(int allowedStatements) throws IllegalStateException {
        super.verifyNotMoreThanOtherThreads(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyExactOtherThreads(int allowedStatements) throws IllegalStateException {
        super.verifyExactOtherThreads(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotLessThanOtherThreads(int allowedStatements) throws IllegalStateException {
        super.verifyNotLessThanOtherThreads(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyRangeOtherThreads(int minAllowedStatements, int maxAllowedStatements) throws IllegalStateException {
        super.verifyRangeOtherThreads(minAllowedStatements, maxAllowedStatements);
        return this;
    }

}
