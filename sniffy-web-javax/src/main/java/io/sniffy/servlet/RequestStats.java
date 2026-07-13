package io.sniffy.servlet;

import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;

import java.util.Map;

/**
 * Package-local compatibility facade over namespace-neutral request statistics.
 */
class RequestStats extends io.sniffy.servlet.internal.RequestStats {

    RequestStats() {
    }

    RequestStats(long timeToFirstByte, long elapsedTime,
                 Map<StatementMetaData, SqlStats> executedStatements) {
        super(timeToFirstByte, elapsedTime, executedStatements);
    }

    RequestStats(long timeToFirstByte, long elapsedTime,
                 Map<StatementMetaData, SqlStats> executedStatements,
                 Map<SocketMetaData, SocketStats> socketOperations) {
        super(timeToFirstByte, elapsedTime, executedStatements, socketOperations);
    }
}
