package io.sniffy.servlet;

import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @see SniffyFilter
 * @since 2.3.0
 */
class RequestStats {

    private long timeToFirstByte;
    private long elapsedTime;
    private Map<StatementMetaData, SqlStats> executedStatements;
    private Map<SocketMetaData, SocketStats> socketOperations;
    private final List<Throwable> exceptions = new CopyOnWriteArrayList<Throwable>();

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

    public int executedStatements() {
        return null == executedStatements ? 0 : exceptions.size();
    }

    public void addExecutedStatements(Map<StatementMetaData, SqlStats> executedStatements) {
        if (null == this.executedStatements) {
            this.executedStatements = executedStatements;
        } else {
            this.executedStatements.putAll(executedStatements);
        }
    }

    public Map<SocketMetaData, SocketStats> getSocketOperations() {
        return socketOperations;
    }

    public void addSocketOperations(Map<SocketMetaData, SocketStats> socketOperations) {
        if (null == this.socketOperations) {
            this.socketOperations = socketOperations;
        } else {
            this.socketOperations.putAll(socketOperations);
        }
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    public void addException(Throwable exception) {
        exceptions.add(exception);
    }

}
