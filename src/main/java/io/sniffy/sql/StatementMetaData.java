package io.sniffy.sql;

import io.sniffy.Query;

/**
 * Represents an executed query - actual SQL, query type (SELECT, INSERT, e.t.c.) and the calling thread
 */
public class StatementMetaData {

    public final String sql;
    public final Query query;
    public final long elapsedTime;
    public final String stackTrace;
    public final long ownerThreadId;

    protected StatementMetaData(String sql, Query query, long elapsedTime, String stackTrace) {
        this.sql = sql;
        this.query = query;
        this.stackTrace = stackTrace;
        this.elapsedTime = elapsedTime;
        this.ownerThreadId = Thread.currentThread().getId();
    }

    public static StatementMetaData parse(String sql) {
        return parse(sql, -1);
    }

    public static StatementMetaData parse(String sql, long elapsedTime) {
        return parse(sql, elapsedTime, null);
    }

    public static StatementMetaData parse(String sql, long elapsedTime, String stackTrace) {
        return new StatementMetaData(null == sql ? null : sql.intern(), guessQueryType(sql), elapsedTime, stackTrace);
    }

    private static Query guessQueryType(String sql) {
        // TODO: some queries can start with "WITH" statement

        for (int i = 0; i < sql.length(); i++) {
            if (!Character.isWhitespace(sql.charAt(i))) {
                String normalized;
                if (sql.length() > (i + 7)) {
                    normalized = sql.substring(i, i + 7).toLowerCase();
                    if (normalized.equals("select ")) {
                        return Query.SELECT;
                    } else if (normalized.equals("insert ")) {
                        return Query.INSERT;
                    } else if (normalized.equals("update ")) {
                        return Query.UPDATE;
                    } else if (normalized.equals("delete ")) {
                        return Query.DELETE;
                    }
                }
                if (sql.length() > (i + 6) && sql.substring(i, i + 6).toLowerCase().equals("merge ")) {
                    return Query.MERGE;
                }
            }
        }

        return Query.OTHER;
    }

}
