package io.sniffy;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;

/**
 * @since 3.1
 */
public abstract class BaseSpy<C extends BaseSpy<C>> {

    protected volatile ConcurrentLinkedHashMap<StatementMetaData, SqlStats> executedStatements =
            new ConcurrentLinkedHashMap.Builder<StatementMetaData, SqlStats>().
                    maximumWeightedCapacity(Long.MAX_VALUE).
                    build();

    protected volatile ConcurrentLinkedHashMap<SocketMetaData, SocketStats> socketOperations =
            new ConcurrentLinkedHashMap.Builder<SocketMetaData, SocketStats>().
                    maximumWeightedCapacity(Long.MAX_VALUE).
                    build();

    public C reset() {
        resetExecutedStatements();
        resetSocketOpertions();
        return self();
    }

    @SuppressWarnings("unchecked")
    protected C self() {
        return (C) this;
    }

    protected void resetExecutedStatements() {
        executedStatements =
                new ConcurrentLinkedHashMap.Builder<StatementMetaData, SqlStats>().
                        maximumWeightedCapacity(Long.MAX_VALUE).
                        build();
    }

    protected void resetSocketOpertions() {
        socketOperations = new ConcurrentLinkedHashMap.Builder<SocketMetaData, SocketStats>().
                maximumWeightedCapacity(Long.MAX_VALUE).
                build();
    }


    protected void addExecutedStatement(StatementMetaData statementMetaData, long elapsedTime, int bytesDown, int bytesUp, int rowsUpdated) {
        SqlStats sqlStats = executedStatements.get(statementMetaData);
        if (null == sqlStats) {
            sqlStats = executedStatements.putIfAbsent(statementMetaData, new SqlStats(elapsedTime, bytesDown, bytesUp, rowsUpdated, 1));
        }
        if (null != sqlStats) {
            sqlStats.accumulate(elapsedTime, bytesDown, bytesUp, rowsUpdated, 1);
        }
    }

    protected void addReturnedRow(StatementMetaData statementMetaData) {
        SqlStats sqlStats = executedStatements.get(statementMetaData);
        if (null != sqlStats) {
            sqlStats.accumulate(0, 0, 0, 1, 0);
        }
    }

    protected void addSocketOperation(SocketMetaData socketMetaData, long elapsedTime, int bytesDown, int bytesUp) {
        SocketStats socketStats = socketOperations.get(socketMetaData);
        if (null == socketStats) {
            socketStats = socketOperations.putIfAbsent(socketMetaData, new SocketStats(elapsedTime, bytesDown, bytesUp));
        }
        if (null != socketStats) {
            socketStats.accumulate(elapsedTime, bytesDown, bytesUp);
        }
    }

}
