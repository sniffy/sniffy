package io.sniffy.servlet;

import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.StatementMetaData;

import java.util.List;
import java.util.Map;

class RequestStats {

    private long timeToFirstByte;
    private long elapsedTime;
    private List<StatementMetaData> executedStatements;
    private Map<SocketMetaData, SocketStats> socketOperations;

    public RequestStats() {
    }

    public RequestStats(long timeToFirstByte, long elapsedTime, List<StatementMetaData> executedStatements) {
        this(timeToFirstByte, elapsedTime, executedStatements, null);
    }

    public RequestStats(
            long timeToFirstByte,
            long elapsedTime,
            List<StatementMetaData> executedStatements,
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

    public List<StatementMetaData> getExecutedStatements() {
        return executedStatements;
    }

    public void setExecutedStatements(List<StatementMetaData> executedStatements) {
        this.executedStatements = executedStatements;
    }

    public Map<SocketMetaData, SocketStats> getSocketOperations() {
        return socketOperations;
    }

    public void setSocketOperations(Map<SocketMetaData, SocketStats> socketOperations) {
        this.socketOperations = socketOperations;
    }

}
