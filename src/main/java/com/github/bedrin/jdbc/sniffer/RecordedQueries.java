package com.github.bedrin.jdbc.sniffer;

public class RecordedQueries {

    private final int executedStatements;
    private final int executedThreadLocalStatements;
    private final int executedOtherThreadsStatements;

    public RecordedQueries(int executedStatements, int executedThreadLocalStatements, int executedOtherThreadsStatements) {
        this.executedStatements = executedStatements;
        this.executedThreadLocalStatements = executedThreadLocalStatements;
        this.executedOtherThreadsStatements = executedOtherThreadsStatements;
    }

    public RecordedQueries verifyNotMore() {
        return verifyNotMoreThan(0);
    }

    public RecordedQueries verifyNotMoreThanOne() {
        return verifyNotMoreThan(1);
    }

    public RecordedQueries verifyNotMoreThan(int allowedStatements) throws AssertionError {
        return verifyRange(0, allowedStatements);
    }

    public RecordedQueries verifyExact(int allowedStatements) throws AssertionError {
        return verifyRange(allowedStatements, allowedStatements);
    }

    public RecordedQueries verifyNotLessThan(int allowedStatements) throws AssertionError {
        return verifyRange(allowedStatements, Integer.MAX_VALUE);
    }

    public RecordedQueries verifyRange(int minAllowedStatements, int maxAllowedStatements) throws AssertionError {
        if (executedStatements > maxAllowedStatements)
            throw new AssertionError(String.format("Allowed not more than %d statements, but actually caught %d statements", maxAllowedStatements, executedStatements));
        if (executedStatements < minAllowedStatements)
            throw new AssertionError(String.format("Allowed not less than %d statements, but actually caught %d statements", minAllowedStatements, executedStatements));
        return this;
    }

    public RecordedQueries verifyNotMoreThreadLocal() {
        return verifyNotMoreThanThreadLocal(0);
    }

    public RecordedQueries verifyNotMoreThanOneThreadLocal() {
        return verifyNotMoreThanThreadLocal(1);
    }

    public RecordedQueries verifyNotMoreThanThreadLocal(int allowedStatements) throws AssertionError {
        return verifyRangeThreadLocal(0, allowedStatements);
    }

    public RecordedQueries verifyExactThreadLocal(int allowedStatements) throws AssertionError {
        return verifyRangeThreadLocal(allowedStatements, allowedStatements);
    }

    public RecordedQueries verifyNotLessThanThreadLocal(int allowedStatements) throws AssertionError {
        return verifyRangeThreadLocal(allowedStatements, Integer.MAX_VALUE);
    }

    public RecordedQueries verifyRangeThreadLocal(int minAllowedStatements, int maxAllowedStatements) throws AssertionError {
        if (executedThreadLocalStatements > maxAllowedStatements)
            throw new AssertionError(String.format("Allowed not more than %d statements in current threads, but actually caught %d statements", maxAllowedStatements, executedThreadLocalStatements));
        if (executedThreadLocalStatements < minAllowedStatements)
            throw new AssertionError(String.format("Allowed not less than %d statements in current threads, but actually caught %d statements", minAllowedStatements, executedThreadLocalStatements));
        return this;
    }

    public RecordedQueries verifyNotMoreOtherThreads() {
        return verifyNotMoreThanOtherThreads(0);
    }

    public RecordedQueries verifyNotMoreThanOneOtherThreads() {
        return verifyNotMoreThanOtherThreads(1);
    }

    public RecordedQueries verifyNotMoreThanOtherThreads(int allowedStatements) throws AssertionError {
        return verifyRangeOtherThreads(0, allowedStatements);
    }

    public RecordedQueries verifyExactOtherThreads(int allowedStatements) throws AssertionError {
        return verifyRangeOtherThreads(allowedStatements, allowedStatements);
    }

    public RecordedQueries verifyNotLessThanOtherThreads(int allowedStatements) throws AssertionError {
        return verifyRangeOtherThreads(allowedStatements, Integer.MAX_VALUE);
    }

    public RecordedQueries verifyRangeOtherThreads(int minAllowedStatements, int maxAllowedStatements) throws AssertionError {
        if (executedOtherThreadsStatements > maxAllowedStatements)
            throw new AssertionError(String.format("Allowed not more than %d statements in current threads, but actually caught %d statements", maxAllowedStatements, executedOtherThreadsStatements));
        if (executedOtherThreadsStatements < minAllowedStatements)
            throw new AssertionError(String.format("Allowed not less than %d statements in current threads, but actually caught %d statements", minAllowedStatements, executedOtherThreadsStatements));
        return this;
    }

}
