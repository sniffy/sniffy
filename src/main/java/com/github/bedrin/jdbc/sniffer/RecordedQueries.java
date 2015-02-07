package com.github.bedrin.jdbc.sniffer;

/**
 * Holds immutable counters with a number of queries executed by given thread, other threads and all threads all together
 */
public class RecordedQueries {

    private final int executedStatements;
    private final int executedThreadLocalStatements;
    private final int executedOtherThreadsStatements;

    RecordedQueries(int executedStatements, int executedThreadLocalStatements, int executedOtherThreadsStatements) {
        this.executedStatements = executedStatements;
        this.executedThreadLocalStatements = executedThreadLocalStatements;
        this.executedOtherThreadsStatements = executedOtherThreadsStatements;
    }

    /**
     * Verifies that no queries has been executed during the observed period
     * @throws AssertionError if actual number of executed statements exceeded 0
     * @since 1.4
     * @return self for the chaining purposes
     */
    public RecordedQueries verifyNotMore() {
        return verifyNotMoreThan(0);
    }

    /**
     * Verifies that at most 1 query has been executed during the observed period
     * @throws AssertionError if actual number of executed statements exceeded 1
     * @since 1.4
     * @return self for the chaining purposes
     */
    public RecordedQueries verifyNotMoreThanOne() {
        return verifyNotMoreThan(1);
    }

    /**
     * Verifies that at most {@code allowedStatements} query has been executed during the observed period
     * @param allowedStatements maximum number of statements which could have been executed during the observed period
     * @throws AssertionError if actual number of executed statements exceeded {@code allowedStatements}
     * @since 1.4
     * @return self for the chaining purposes
     */
    public RecordedQueries verifyNotMoreThan(int allowedStatements) throws AssertionError {
        return verifyRange(0, allowedStatements);
    }

    /**
     * Verifies that exactly {@code allowedStatements} queries has been executed during the observed period
     * @param allowedStatements number of statements which could have been executed during the observed period
     * @throws AssertionError
     * @since 1.4
     * @return self for the chaining purposes
     */
    public RecordedQueries verifyExact(int allowedStatements) throws AssertionError {
        return verifyRange(allowedStatements, allowedStatements);
    }

    /**
     * Verifies that at least {@code allowedStatements} queries has been executed during the observed period
     * @param allowedStatements minimum number of statements which could have been executed during the observed period
     * @since 1.4
     * @return self for the chaining purposes
     */
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
