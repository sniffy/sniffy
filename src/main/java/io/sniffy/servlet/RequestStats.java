package io.sniffy.servlet;

import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;

import java.util.List;
import java.util.Map;

class RequestStats {

    private long timeToFirstByte;
    private long elapsedTime;
    private Map<StatementMetaData, SqlStats> executedStatements;
    private Map<SocketMetaData, SocketStats> socketOperations;

    public RequestStats() {
    }

    public RequestStats(long timeToFirstByte, long elapsedTime, Map<StatementMetaData, SqlStats> executedStatements) {
        this(timeToFirstByte, elapsedTime, executedStatements, null);
    }

    public RequestStats(
            long timeToFirstByte,
            long elapsedTime,
            Map<StatementMetaData, SqlStats> executedStatements,
            Map<SocketMetaData, SocketStats> socketOperations) {
        this.timeToFirstByte = timeToFirstByte;
        this.elapsedTime = elapsedTime;
        this.executedStatements = executedStatements;
        this.socketOperations = socketOperations;
    }

    public long getTimeToFirstByte() {
        return timeToFirstByte;
    }

    public void setTimeToFirstByte(long timeToFirstByte) {
        this.timeToFirstByte = timeToFirstByte;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public Map<StatementMetaData, SqlStats> getExecutedStatements() {
        return executedStatements;
    }

    public void setExecutedStatements(Map<StatementMetaData, SqlStats> executedStatements) {
        this.executedStatements = executedStatements;
    }

    public Map<SocketMetaData, SocketStats> getSocketOperations() {
        return socketOperations;
    }

    public void setSocketOperations(Map<SocketMetaData, SocketStats> socketOperations) {
        this.socketOperations = socketOperations;
    }

}
