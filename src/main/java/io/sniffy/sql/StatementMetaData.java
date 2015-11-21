package io.sniffy.sql;

import io.sniffy.Query;

/**
 * Represents an executed query - actual SQL, query type (SELECT, INSERT, e.t.c.) and the calling thread
 */
public class StatementMetaData {

    public final String sql;
    public final Query query;
    public final long elapsedTime;
    public final Thread owner;


    protected StatementMetaData(String sql, Query query, long elapsedTime) {
        this.sql = sql;
        this.query = query;
        this.elapsedTime = elapsedTime;
        this.owner = Thread.currentThread();
    }

    public static StatementMetaData parse(String sql) {
        return parse(sql, -1);
    }

    public static StatementMetaData parse(String sql, long elapsedTime) {

        if (null == sql) return null;

        String normalized = sql.trim().toLowerCase();

        Query query;

        if (normalized.startsWith("select ")) {
            // TODO: can start with "WITH" statement
            query = Query.SELECT;
        } else if (normalized.startsWith("insert ")) {
            query = Query.INSERT;
        } else if (normalized.startsWith("update ")) {
            query = Query.UPDATE;
        } else if (normalized.startsWith("delete ")) {
            query = Query.DELETE;
        } else if (normalized.startsWith("merge ")) {
            // TODO: can start with "WITH" statement
            query = Query.MERGE;
        } else {
            query = Query.OTHER;
        }

        return new StatementMetaData(sql, query, elapsedTime);
    }

}
