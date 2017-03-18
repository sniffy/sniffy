package io.sniffy.sql;

/**
 * Represents an executed query - actual SQL, query type (SELECT, INSERT, e.t.c.) and the calling thread
 * @since 3.0.0
 */
public class StatementMetaData {

    public final String sql;
    public final SqlStatement query;
    public final String stackTrace;
    public final long ownerThreadId;

    private final int hashCode;

    public StatementMetaData(String sql, SqlStatement query, String stackTrace, long ownerThreadId) {
        this.sql = null == sql ? null : sql.intern();
        this.query = query;
        this.stackTrace = null == stackTrace ? null : stackTrace.intern();
        this.ownerThreadId = ownerThreadId;

        hashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = System.identityHashCode(sql);
        result = 31 * result + query.hashCode();
        result = 31 * result + System.identityHashCode(stackTrace);
        result = 31 * result + (int) (ownerThreadId ^ (ownerThreadId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatementMetaData that = (StatementMetaData) o;

        if (ownerThreadId != that.ownerThreadId) return false;
        if (sql != that.sql) return false;
        if (query != that.query) return false;
        return stackTrace == that.stackTrace;

    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
