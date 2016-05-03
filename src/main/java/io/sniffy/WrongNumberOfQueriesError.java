package io.sniffy;

import io.sniffy.sql.StatementMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.sniffy.util.StringUtil.LINE_SEPARATOR;

/**
 * @since 2.0
 */
public class WrongNumberOfQueriesError extends SniffyAssertionError {

    private final Threads threadMatcher;
    private final Query query;
    private final int minimumQueries;
    private final int maximumQueries;
    private final int numQueries;
    private final List<StatementMetaData> executedStatements;

    public WrongNumberOfQueriesError(
            Threads threadMatcher, Query query,
            int minimumQueries, int maximumQueries, int numQueries,
            List<StatementMetaData> executedStatements) {
        super(buildDetailMessage(threadMatcher, query, minimumQueries, maximumQueries, numQueries, executedStatements));
        this.threadMatcher = threadMatcher;
        this.query = query;
        this.minimumQueries = minimumQueries;
        this.maximumQueries = maximumQueries;
        this.numQueries = numQueries;
        this.executedStatements = Collections.unmodifiableList(executedStatements);
    }

    public Threads getThreadMatcher() {
        return threadMatcher;
    }

    public Query getQuery() {
        return query;
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
            Threads threadMatcher, Query query,
            int minimumQueries, int maximumQueries, int numQueries,
            List<StatementMetaData> executedStatements) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected between ").append(minimumQueries).append(" and ").append(maximumQueries);
        if (Threads.CURRENT == threadMatcher) {
            sb.append(" current thread");
        } else if (Threads.OTHERS == threadMatcher) {
            sb.append(" other threads");
        }
        if (Query.ANY != query && null != query) {
            sb.append(" ").append(query);
        }
        sb.append(" queries").append(LINE_SEPARATOR);
        sb.append("Observed ").append(numQueries).append(" queries instead:").append(LINE_SEPARATOR);
        if (null != executedStatements) for (StatementMetaData statement : executedStatements) {
            if (Query.ANY == query || null == query || statement.query == query) {
                sb.append(statement.sql).append(';').append(LINE_SEPARATOR);
            }
        }
        return sb.toString();
    }

}
