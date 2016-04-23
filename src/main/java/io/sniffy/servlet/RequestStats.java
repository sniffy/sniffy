package io.sniffy.servlet;

import io.sniffy.sql.StatementMetaData;

import java.util.List;

class RequestStats {

    private long timeToFirstByte;
    private long elapsedTime;
    private List<StatementMetaData> executedStatements;

    public RequestStats() {
    }

    public RequestStats(List<StatementMetaData> executedStatements) {
        this.executedStatements = executedStatements;
    }

    public RequestStats(long timeToFirstByte, long elapsedTime, List<StatementMetaData> executedStatements) {
        this.timeToFirstByte = timeToFirstByte;
        this.elapsedTime = elapsedTime;
        this.executedStatements = executedStatements;
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

}
