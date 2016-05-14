package io.sniffy.sql;

import io.sniffy.Threads;

import java.util.Collection;

import static io.sniffy.util.StringUtil.LINE_SEPARATOR;

/**
 * @since 3.1
 */
public class WrongNumberOfQueriesError extends io.sniffy.WrongNumberOfQueriesError {

    public WrongNumberOfQueriesError(
            Threads threadMatcher, SqlStatement query,
            int minimumQueries, int maximumQueries, int numQueries,
            Collection<StatementMetaData> executedStatements) {
        super(
                buildDetailMessage(threadMatcher, query, minimumQueries, maximumQueries, numQueries, executedStatements),
                threadMatcher, query,
                minimumQueries, maximumQueries, numQueries,
                executedStatements
        );
    }

    private static String buildDetailMessage(
            Threads threadMatcher, SqlStatement query,
            int minimumQueries, int maximumQueries, int numQueries,
            Collection<StatementMetaData> executedStatements) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected between ").append(minimumQueries).append(" and ").append(maximumQueries);
        if (Threads.CURRENT == threadMatcher) {
            sb.append(" current thread");
        } else if (Threads.OTHERS == threadMatcher) {
            sb.append(" other threads");
        }
        if (SqlStatement.ANY != query && null != query) {
            sb.append(" ").append(query);
        }
        sb.append(" queries").append(LINE_SEPARATOR);
        sb.append("Observed ").append(numQueries).append(" queries instead:").append(LINE_SEPARATOR);
        if (null != executedStatements) for (StatementMetaData statement : executedStatements) {
            if (SqlStatement.ANY == query || null == query || statement.query == query) {
                sb.append(statement.sql).append(';').append(LINE_SEPARATOR);
            }
        }
        return sb.toString();
    }

}
