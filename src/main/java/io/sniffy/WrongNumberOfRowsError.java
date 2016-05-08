package io.sniffy;

import io.sniffy.sql.StatementMetaData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.sniffy.util.StringUtil.LINE_SEPARATOR;

/**
 * @since 2.0
 */
public class WrongNumberOfRowsError extends SniffyAssertionError {

    private final Threads threadMatcher;
    private final Query query;
    private final int minimumRows;
    private final int maximumRows;
    private final int numRows;
    private final Collection<StatementMetaData> executedStatements;

    public WrongNumberOfRowsError(
            Threads threadMatcher, Query query,
            int minimumRows, int maximumQueries, int numRows,
            Collection<StatementMetaData> executedStatements) {
        super(buildDetailMessage(threadMatcher, query, minimumRows, maximumQueries, numRows, executedStatements));
        this.threadMatcher = threadMatcher;
        this.query = query;
        this.minimumRows = minimumRows;
        this.maximumRows = maximumQueries;
        this.numRows = numRows;
        this.executedStatements = Collections.unmodifiableCollection(executedStatements);
    }

    public Threads getThreadMatcher() {
        return threadMatcher;
    }

    public Query getQuery() {
        return query;
    }

    public int getMinimumRows() {
        return minimumRows;
    }

    public int getMaximumRows() {
        return maximumRows;
    }

    public int getNumRows() {
        return numRows;
    }

    /**
     * @since 2.3.1
     * @return
     */
    public Collection<StatementMetaData> getExecutedStatements() {
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
            Collection<StatementMetaData> executedStatements) {
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
        sb.append(" rows returned / affected").append(LINE_SEPARATOR);
        sb.append("Observed ").append(numQueries).append(" rows instead:");
        return sb.toString();
    }

}
