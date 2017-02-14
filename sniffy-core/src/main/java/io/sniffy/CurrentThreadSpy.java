package io.sniffy;

import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStatement;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * @since 3.1
 */
public class CurrentThreadSpy extends BaseSpy<CurrentThreadSpy> implements Closeable {

    public CurrentThreadSpy() {
        Sniffy.registerCurrentThreadSpy(this);
    }

    public int executedStatements() {
        int count = 0;

        if (null != executedStatements) for (Map.Entry<StatementMetaData, SqlStats> entry : executedStatements.entrySet()) {
            StatementMetaData statementMetaData = entry.getKey();
            SqlStats sqlStats = entry.getValue();
            if (statementMetaData.query != SqlStatement.SYSTEM) {
                count += sqlStats.queries.intValue();
            }
        }

        return count;
    }

    public Map<StatementMetaData, SqlStats> getExecutedStatements() {
        return executedStatements;
    }

    public Map<SocketMetaData, SocketStats> getSocketOperations() {
        return socketOperations;
    }

    @Override
    public void close() throws IOException {
        Sniffy.removeCurrentThreadSpyReference();
    }

}
