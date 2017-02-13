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

    public static SqlStatement guessQueryType(String sql) {
        // TODO: some queries can start with "WITH" statement
        // TODO: move to StatementInvocationHandler

        for (int i = 0; i < sql.length(); i++) {
            if (!Character.isWhitespace(sql.charAt(i))) {
                String normalized;
                if (sql.length() > (i + 7)) {
                    normalized = sql.substring(i, i + 7).toLowerCase();
                    if (normalized.equals("select ")) {
                        return SqlStatement.SELECT;
                    } else if (normalized.equals("insert ")) {
                        return SqlStatement.INSERT;
                    } else if (normalized.equals("update ")) {
                        return SqlStatement.UPDATE;
                    } else if (normalized.equals("delete ")) {
                        return SqlStatement.DELETE;
                    }
                }
                if (sql.length() > (i + 6) && sql.substring(i, i + 6).toLowerCase().equals("merge ")) {
                    return SqlStatement.MERGE;
                }
            }
        }

        return SqlStatement.OTHER;
    }

}
