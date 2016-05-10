package io.sniffy.sql;

import io.sniffy.Query;

/**
 * Represents an executed query - actual SQL, query type (SELECT, INSERT, e.t.c.) and the calling thread
 */
public class StatementMetaData {

    public final String sql;
    public final Query query;
    public final String stackTrace;
    public final long ownerThreadId;

    public StatementMetaData(String sql, Query query, String stackTrace, long ownerThreadId) {
        this.sql = sql;
        this.query = query;
        this.stackTrace = null == stackTrace ? null : stackTrace.intern();
        this.ownerThreadId = ownerThreadId;
    }

    public static Query guessQueryType(String sql) {
        // TODO: some queries can start with "WITH" statement
        // TODO: move to StatementInvocationHandler

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatementMetaData that = (StatementMetaData) o;

        if (ownerThreadId != that.ownerThreadId) return false;
        if (sql != null ? !sql.equals(that.sql) : that.sql != null) return false;
        if (query != that.query) return false;
        return stackTrace != null ? stackTrace.equals(that.stackTrace) : that.stackTrace == null;

    }

    @Override
    public int hashCode() {
        int result = sql != null ? sql.hashCode() : 0;
        result = 31 * result + query.hashCode();
        result = 31 * result + (stackTrace != null ? stackTrace.hashCode() : 0);
        result = 31 * result + (int) (ownerThreadId ^ (ownerThreadId >>> 32));
        return result;
    }

}
