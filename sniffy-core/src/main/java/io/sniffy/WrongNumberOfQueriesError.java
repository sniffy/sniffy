package io.sniffy;

import io.sniffy.sql.SqlStatement;
import io.sniffy.sql.StatementMetaData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @see io.sniffy.sql.WrongNumberOfQueriesError
 * @since 2.0
 */
@Deprecated
public class WrongNumberOfQueriesError extends SniffyAssertionError {

    protected final Threads threadMatcher;
    protected final SqlStatement query;
    protected final int minimumQueries;
    protected final int maximumQueries;
    protected final int numQueries;
    protected final Collection<StatementMetaData> executedStatements;

    public WrongNumberOfQueriesError(
            String message,
            Threads threadMatcher, SqlStatement query,
            int minimumQueries, int maximumQueries, int numQueries,
            Collection<StatementMetaData> executedStatements) {
        super(message);
        this.threadMatcher = threadMatcher;
        this.query = query;
        this.minimumQueries = minimumQueries;
        this.maximumQueries = maximumQueries;
        this.numQueries = numQueries;
        this.executedStatements = Collections.unmodifiableCollection(executedStatements);
    }

    public Threads getThreadMatcher() {
        return threadMatcher;
    }

    public SqlStatement getQuery() {
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

}
