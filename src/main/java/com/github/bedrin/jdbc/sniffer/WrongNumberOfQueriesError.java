package com.github.bedrin.jdbc.sniffer;

import java.util.Collections;
import java.util.List;

/**
 * @since 2.0
 */
public class WrongNumberOfQueriesError extends AssertionError {

    private final static String LINE_SEPARATOR = System.getProperty("line.separator");

    private final Threads threadMatcher;
    private final int minimumQueries;
    private final int maximumQueries;
    private final int numQueries;
    private final List<String> executedSqls;

    public WrongNumberOfQueriesError(Threads threadMatcher, int minimumQueries, int maximumQueries, int numQueries, List<String> executedSqls) {
        super(buildDetailMessage(threadMatcher, minimumQueries, maximumQueries, numQueries, executedSqls));
        this.threadMatcher = threadMatcher;
        this.minimumQueries = minimumQueries;
        this.maximumQueries = maximumQueries;
        this.numQueries = numQueries;
        this.executedSqls = Collections.unmodifiableList(executedSqls);
    }

    public Threads getThreadMatcher() {
        return threadMatcher;
    }

    public int getMinimumQueries() {
        return minimumQueries;
    }

    public int getMaximumQueries() {
        return maximumQueries;
    }

    public int getNumQueries() {
        return numQueries;
    }

    public List<String> getExecutedSqls() {
        return executedSqls;
    }

    private static String buildDetailMessage(
            Threads threadMatcher, int minimumQueries, int maximumQueries, int numQueries, List<String> executedSqls) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected between ").append(minimumQueries).append(" and ").append(maximumQueries);
        if (Threads.CURRENT == threadMatcher) {
            sb.append(" current thread ");
        } else if (Threads.OTHERS == threadMatcher) {
            sb.append(" other threads ");
        }
        sb.append(" queries").append(LINE_SEPARATOR);
        sb.append("Observer ").append(numQueries).append(" queries instead:").append(LINE_SEPARATOR);
        if (null != executedSqls) for (String sql : executedSqls) {
            sb.append(sql).append(';').append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

}
