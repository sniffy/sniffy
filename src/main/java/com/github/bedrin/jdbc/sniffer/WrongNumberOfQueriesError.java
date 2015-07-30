package com.github.bedrin.jdbc.sniffer;

import com.github.bedrin.jdbc.sniffer.sql.StatementMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.bedrin.jdbc.sniffer.util.StringUtil.LINE_SEPARATOR;

/**
 * @since 2.0
 */
public class WrongNumberOfQueriesError extends AssertionError {

    private final Threads threadMatcher;
    private final int minimumQueries;
    private final int maximumQueries;
    private final int numQueries;
    private final List<StatementMetaData> executedStatements;

    public WrongNumberOfQueriesError(
            Threads threadMatcher,
            int minimumQueries, int maximumQueries, int numQueries,
            List<StatementMetaData> executedStatements) {
        super(buildDetailMessage(threadMatcher, minimumQueries, maximumQueries, numQueries, executedStatements));
        this.threadMatcher = threadMatcher;
        this.minimumQueries = minimumQueries;
        this.maximumQueries = maximumQueries;
        this.numQueries = numQueries;
        this.executedStatements = Collections.unmodifiableList(executedStatements);
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

    /**
     * @since 2.3.1
     * @return
     */
    public List<StatementMetaData> getExecutedStatements() {
        return executedStatements;
    }

    public List<String> getExecutedSqls() {
        List<String> executedSqls = new ArrayList<String>(executedStatements.size());
        for (StatementMetaData statement : executedStatements) {
            executedSqls.add(statement.sql);
        }
        return executedSqls;
    }

    private static String buildDetailMessage(
            Threads threadMatcher,
            int minimumQueries, int maximumQueries, int numQueries,
            List<StatementMetaData> executedStatements) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected between ").append(minimumQueries).append(" and ").append(maximumQueries);
        if (Threads.CURRENT == threadMatcher) {
            sb.append(" current thread ");
        } else if (Threads.OTHERS == threadMatcher) {
            sb.append(" other threads ");
        }
        sb.append(" queries").append(LINE_SEPARATOR);
        sb.append("Observed ").append(numQueries).append(" queries instead:").append(LINE_SEPARATOR);
        if (null != executedStatements) for (StatementMetaData statement : executedStatements) {
            sb.append(statement.sql).append(';').append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

}
