package com.github.bedrin.jdbc.sniffer;

public class RecordedQueriesWithValue<T> extends RecordedQueries {

    private final T value;

    RecordedQueriesWithValue(T value, int executedStatements, int executedThreadLocalStatements, int executedOtherThreadsStatements) {
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
    public RecordedQueriesWithValue verifyNotMoreThan(int allowedStatements) throws AssertionError {
        super.verifyNotMoreThan(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyExact(int allowedStatements) throws AssertionError {
        super.verifyExact(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotLessThan(int allowedStatements) throws AssertionError {
        super.verifyNotLessThan(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyRange(int minAllowedStatements, int maxAllowedStatements) throws AssertionError {
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
    public RecordedQueriesWithValue verifyNotMoreThanThreadLocal(int allowedStatements) throws AssertionError {
        super.verifyNotMoreThanThreadLocal(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyExactThreadLocal(int allowedStatements) throws AssertionError {
        super.verifyExactThreadLocal(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotLessThanThreadLocal(int allowedStatements) throws AssertionError {
        super.verifyNotLessThanThreadLocal(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyRangeThreadLocal(int minAllowedStatements, int maxAllowedStatements) throws AssertionError {
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
    public RecordedQueriesWithValue verifyNotMoreThanOtherThreads(int allowedStatements) throws AssertionError {
        super.verifyNotMoreThanOtherThreads(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyExactOtherThreads(int allowedStatements) throws AssertionError {
        super.verifyExactOtherThreads(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyNotLessThanOtherThreads(int allowedStatements) throws AssertionError {
        super.verifyNotLessThanOtherThreads(allowedStatements);
        return this;
    }

    @Override
    public RecordedQueriesWithValue verifyRangeOtherThreads(int minAllowedStatements, int maxAllowedStatements) throws AssertionError {
        super.verifyRangeOtherThreads(minAllowedStatements, maxAllowedStatements);
        return this;
    }

}
