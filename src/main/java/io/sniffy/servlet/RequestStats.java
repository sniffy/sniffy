package io.sniffy.servlet;

import io.sniffy.sql.StatementMetaData;

import java.util.List;

class RequestStats {

    private long elapsedTime;
    private List<StatementMetaData> executedStatements;

    public RequestStats() {
    }

    public RequestStats(List<StatementMetaData> executedStatements) {
        this.executedStatements = executedStatements;
    }

    public RequestStats(long elapsedTime, List<StatementMetaData> executedStatements) {
        this.elapsedTime = elapsedTime;
        this.executedStatements = executedStatements;
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
